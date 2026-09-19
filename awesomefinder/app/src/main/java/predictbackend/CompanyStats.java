

public class CompanyStats {
    public static CompanyStats processSqlResult(ResultSet rs) throws SQLException {
        int currentCount = rs.getInt("current_30d_count");
        int prevCount = rs.getInt("prev_60d_count");
        LocalDate latestDate = rs.getDate("latest_posting_date").toLocalDate();

        // 1. Calculate percentage velocity change in Java
        double velocityChange = 0.0;
        if (prevCount > 0) {
            velocityChange = ((double) (currentCount - prevCount) / prevCount) * 100.0;
        }

        // 2. Calculate inactivity days
        long daysInactive = ChronoUnit.DAYS.between(latestDate, LocalDate.now());

        // 3. Construct clean object ready for Jackson serialization
        return new CompanyStats(currentCount, prevCount, velocityChange, daysInactive);
    }
}