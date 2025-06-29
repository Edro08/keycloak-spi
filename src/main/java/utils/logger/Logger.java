package utils.logger;

import org.keycloak.services.ServicesLogger;

public class Logger implements ILogger {
    protected static final ServicesLogger log = ServicesLogger.LOGGER;
    private static final String SEPARATOR = " | ";
    private static final String KEY_VALUE_SEPARATOR = ": ";

    @Override
    public void Info(String title, Object... args) {
        log.infov(buildMessage(title, args));
    }

    @Override
    public void Error(String title, Object... args) {
        log.errorv(buildMessage(title, args));
    }

    @Override
    public void Error(String title, Throwable t, Object... args) {
        log.errorv(t, buildMessage(title, args));
    }

    @Override
    public void Warn(String title, Object... args) {
        log.warnv(buildMessage(title, args));
    }

    private String buildMessage(String title, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (args != null && args.length > 0) {
            sb.append(SEPARATOR);
            for (int i = 0; i < args.length; i += 2) {
                // clave (toString seguro)
                String key = (args[i] != null) ? args[i].toString() : "null";
                sb.append(key).append(KEY_VALUE_SEPARATOR);

                // valor (toString seguro o "null")
                if (i + 1 < args.length) {
                    Object val = args[i + 1];
                    sb.append(val != null ? val.toString() : "null");
                } else {
                    sb.append("null");
                }

                if (i + 2 < args.length) sb.append(SEPARATOR);
            }
        }
        return sb.toString();
    }
}
