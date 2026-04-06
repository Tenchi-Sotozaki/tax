package jp.lg.asp.accommodation.exception;

public class MonthlyReportNotFoundException extends BusinessException {

    public MonthlyReportNotFoundException(String paymentYearMonth) {
        super("ERR_MONTHLY_REPORT_NOT_FOUND",
                String.format("指定された納入年月（%s）の月計表が登録されていません。先に月計表を登録してください。",
                        paymentYearMonth));
    }
}
