package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.metadata.Currency;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataRepositoryImpl implements MetadataRepository {

  @Autowired JdbcTemplate jdbcTemplate;

  @Override
  public List<Currency> getCurrencyRates(
      String baseCurrency, Timestamp validFrom, Timestamp validTo) {
    String query =
        """
					select
						*
					from
						public.currency_rates
					where
						(?::text is null or currency_code = ?)
						and vt_begin >= ?
						and vt_end <= ?
				""";
    return jdbcTemplate.query(
        query,
        rs -> {
          List<Currency> currencyList = new ArrayList<>();
          while (rs.next()) {
            try {
              currencyList.add(getCurrencyFromResultSet(rs));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
          return currencyList;
        },
        baseCurrency,
        baseCurrency,
        validFrom,
        validTo);
  }

  @Override
  public void setCurrencyRates(Currency currency) {
    Double rate = currency.getRate();
    String baseCurrency = currency.getBaseCurrency();
    Timestamp validFrom = currency.getValidFrom();
    Timestamp validTo = currency.getValidTo();
    LocalDate from = validFrom.toLocalDateTime().toLocalDate();
    LocalDate to = validTo.toLocalDateTime().toLocalDate();
    for (LocalDate date = from; date.isBefore(to); date = date.plusMonths(1)) {
      validFrom = Timestamp.valueOf(date.atStartOfDay());
      validTo = Timestamp.valueOf((date.plusMonths(1)).atStartOfDay());
      String query =
          """
						delete from
							public.currency_rates
						where vt_begin = ?
							and vt_end = ?
							and currency_code = ?;

						INSERT INTO public.currency_rates (currency_code, usd_equivalent, vt_begin, vt_end)
						VALUES (?, ?, ?, ?);
					""";
      jdbcTemplate.update(
          query, validFrom, validTo, baseCurrency, baseCurrency, rate, validFrom, validTo);
    }
  }

  private Currency getCurrencyFromResultSet(ResultSet rs) throws SQLException, IOException {
    return Currency.builder()
        .baseCurrency(rs.getString("currency_code"))
        .toCurrency("USD")
        .rate(rs.getDouble("usd_equivalent"))
        .validFrom(rs.getTimestamp("vt_begin"))
        .validTo(rs.getTimestamp("vt_end"))
        .build();
  }
}
