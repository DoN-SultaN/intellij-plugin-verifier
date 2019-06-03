import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.util.FileSize

statusListener OnConsoleStatusListener

String timedPattern = "%20(%d{yyyy-MM-dd'T'HH:mm:ss}) %-22([%thread]) %-5level %-30.30(%logger{0}:%method) - %msg%n"

//https://logback.qos.ch/manual/groovy.html
appender('STDOUT', ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
}

def home = System.getProperty('verifier.service.home.directory') ?: System.getProperty("user.home") + File.separator + ".verifier-service"

appender('FILE', RollingFileAppender) {
  //the logger is always logging into this file (but rollovers the files according to fileNamePattern)
  file = "$home/log/verifier.log"

  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "$home/log/verifier/%d{MM/dd, aux}/verifier.%d{yyyy-MM-dd_HH}.%i.log"
    //keep logs for 30 days
    maxHistory = 30 * 24
    maxFileSize = FileSize.valueOf("100MB")
    totalSizeCap = FileSize.valueOf("2GB")
  }

  append = true
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }

}

appender('ERRORS_FILE', RollingFileAppender) {
  file = "$home/log/errors.log"
  rollingPolicy(SizeAndTimeBasedRollingPolicy) {
    fileNamePattern = "$home/log/errors/%d{MM/dd, aux}/errors.%d{yyyy-MM-dd}.%i.log"
    maxHistory = 30
    maxFileSize = FileSize.valueOf("100MB")
    totalSizeCap = FileSize.valueOf("2GB")
  }
  append = true
  encoder(PatternLayoutEncoder) {
    pattern = timedPattern
  }
  filter(ThresholdFilter) {
    level = WARN
  }

}

boolean developmentMode = 'true' == System.getProperty('verifier.service.development.mode')
def loggers = ['STDOUT', 'ERRORS_FILE', 'FILE']

//Configure used libraries' loggers.
logger("com.jetbrains.pluginverifier", WARN, loggers, false)
logger("com.jetbrains.plugin.structure", WARN, loggers, false)
logger("com.jetbrains.intellij.feature.extractor", WARN, loggers, false)
logger("FeaturesExtractor", developmentMode ? DEBUG : ERROR, loggers, false)

//Configure the service's loggers.
logger("org.jetbrains.plugins.verifier.service", developmentMode ? DEBUG : INFO, loggers, false)

root(INFO, loggers)