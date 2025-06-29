package utils.logger;

public interface ILogger {
    void Error(String Title, Object... args);
    void Error(String title, Throwable t, Object... args);
    void Info(String Title, Object... args);
    void Warn(String Title, Object... args);
}
