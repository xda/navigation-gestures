#!/system/bin/sh

export CLASSPATH="$(expr $(pm path com.xda.nobar) : "package:\(.*\)")"

exec app_process /system/bin --nice-name=NoBarKey com.xda.nobar.util.KeyInjector "$@"