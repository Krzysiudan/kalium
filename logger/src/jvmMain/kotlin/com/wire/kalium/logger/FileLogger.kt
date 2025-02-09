/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.kalium.logger

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import java.io.File
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private var LOG_FILE_FLUSH_PERIOD = 5.seconds.toLong(DurationUnit.MILLISECONDS)

class FileLogger(outputfile: File) : LogWriter() {

    private val writer = outputfile.bufferedWriter()

    init {
        // Attempt to close & flush the output file before the process terminates
        Runtime.getRuntime().addShutdownHook(
            Thread {
                writer.close()
            }
        )

        // Flush the output file every 5 seconds for the case when there a low log output
        Timer().scheduleAtFixedRate(delay = LOG_FILE_FLUSH_PERIOD, period = LOG_FILE_FLUSH_PERIOD) {
            writer.flush()
        }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val str = "$severity: ($tag) $message"
        writer.write(str)
        writer.newLine()

        throwable?.let {
            val thString = it.stackTraceToString()
            writer.write(thString)
            writer.newLine()
        }
    }

}
