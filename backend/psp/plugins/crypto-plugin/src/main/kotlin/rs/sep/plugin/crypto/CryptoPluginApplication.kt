package rs.sep.plugin.crypto

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CryptoPluginApplication

fun main(args: Array<String>) {
    runApplication<CryptoPluginApplication>(*args)
}
