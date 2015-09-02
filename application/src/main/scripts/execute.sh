#!/bin/sh

if test -z "$JAVA_BIN"; then
    if test -z "$JAVA_HOME"; then
        JAVA_BIN="java"
    else
        JAVA_BIN="$JAVA_HOME/bin/java"
    fi
fi

CLASSPATH="$(echo lib/*.jar | tr " " ":")"

case "$1" in
    start)
        nohup "$JAVA_BIN" -classpath "${CLASSPATH}" com.fithian.application.Main  > /dev/null 2>&1 &
        echo $! > /tmp/app.pid
        ;;

    console)
        "$JAVA_BIN" -classpath "${CLASSPATH}" com.fithian.application.Main
        ;;

    stop)
        kill -15 `cat /tmp/app.pid`
        ;;

    *)
        echo "Usage:"
        echo "$0 (start|stop)"
        echo "        start   - start app in the background"
        echo "        console - start app in the foreground"
        echo "        stop    - stop app"
        ;;
esac
