#!/system/bin/sh

export CLASSPATH=$(expr $(pm path com.xda.nobar) : "package:\(.*\)")

exec app_process /system/bin com.xda.nobar.util.KeyInjector "$@"