package de.hausbus.homematic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HausBusLogFormatter extends Formatter
{
    private static final MessageFormat logFormat = new MessageFormat("{0,date,dd}.{0,date,MM}.{0,date,yyyy} {0,time,HH}:{0,time,mm}:{0,time,ss}.{0,time,SSS}");
    private static final String lineSeparator = System.getProperty("line.separator");
    private static final Map<Integer, ThreadIdName> threadIdentifiers = new HashMap<Integer, ThreadIdName>();
    private static final Map<String, Integer> oldEntries = new HashMap<String, Integer>();
    private static final Object semaphor = new Object();

    @Override
    public String format(LogRecord record)
    {
        ThreadIdName tin = null;

        synchronized (semaphor)
        {
            if (!threadIdentifiers.containsKey(record.getThreadID()) && threadIdentifiers.size() < 1000 && oldEntries.size() < 1000)
            {
                String message = record.getMessage();
                if (message != null && message.length() > 19 && message.substring(0, 19).equals("THREAD IDENTIFIER: "))
                {
                    String identString = message.substring(19);
                    int pos = identString.indexOf("#");
                    if (pos != -1)
                    {
                        String threadId = identString.substring(0, pos);
                        String threadName = identString.substring(pos + 1);
                        threadIdentifiers.put(record.getThreadID(), new ThreadIdName(threadId, threadName));

                        if (oldEntries.containsKey(threadName))
                            threadIdentifiers.remove(oldEntries.get(threadName));

                        oldEntries.put(threadName, record.getThreadID());
                    }
                }
            }
            tin = threadIdentifiers.get(record.getThreadID());
        }

        StringBuilder sb = new StringBuilder();
        Date date = new Date();
        Object[] args = { date };

        // Minimize memory allocations here.
        date.setTime(record.getMillis());
        StringBuffer text = new StringBuffer();
        logFormat.format(args, text, null);
        sb.append(text).append(" ");

        String method = null;
        if (record.getSourceClassName() != null)
        {
            String sourceClass = record.getSourceClassName();
            if (sourceClass.contains("."))
                sourceClass = sourceClass.substring(sourceClass.lastIndexOf(".") + 1, sourceClass.length());
            method = sourceClass;
        }

        if (record.getSourceMethodName() != null)
            method += "." + record.getSourceMethodName();

        if (record.getLevel() == Level.INFO)
            sb.append("(INFO) ");
        else if (record.getLevel() == Level.SEVERE)
            sb.append("(SEVERE) ");
        else if (record.getLevel() == Level.WARNING)
            sb.append("(WARNING) ");

        sb.append(formatMessage(record)).append(" ");
        if (method != null)
            sb.append(method).append(" ");

        if (record.getThrown() != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            sb.append(lineSeparator);
            sb.append(sw.toString());
            sb.append(" ");
        }

        if (record.getLevel() == Level.FINE)
            sb.append("FINE");
        else if (record.getLevel() == Level.FINER)
            sb.append("FINER");
        else if (record.getLevel() == Level.FINEST)
            sb.append("FINEST");
        else if (record.getLevel() == Level.INFO)
            sb.append("INFO");
        else if (record.getLevel() == Level.SEVERE)
            sb.append("SEVERE");
        else if (record.getLevel() == Level.WARNING)
            sb.append("WARNING");
        else if (record.getLevel() == Level.CONFIG)
            sb.append("CONFIG");
        else
            sb.append(record.getLevel());

        sb.append(" ").append(record.getThreadID());

        if (tin != null)
            sb.append(" [").append(tin.name).append("]");

        sb.append(" ").append(record.getLoggerName());

        sb.append(lineSeparator);
        return sb.toString();
    }

    class ThreadIdName
    {
        private final String id;
        private final String name;

        ThreadIdName(String id, String name)
        {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ThreadIdName other = (ThreadIdName) obj;
            if (id == null)
            {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (name == null)
            {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }
}
