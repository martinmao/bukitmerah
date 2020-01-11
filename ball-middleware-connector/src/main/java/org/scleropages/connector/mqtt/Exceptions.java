/**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.connector.mqtt;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Exceptions {

    public static class MqttException extends RuntimeException {

        private final org.eclipse.paho.client.mqttv3.MqttException nativeException;

        public MqttException(org.eclipse.paho.client.mqttv3.MqttException nativeException) {
            super(nativeException);
            this.nativeException = nativeException;
        }

        /**
         * Returns the reason code for this exception.
         *
         * @return the code representing the reason for this exception.
         */
        public int getReasonCode() {
            return nativeException.getReasonCode();
        }

        /**
         * Returns the underlying cause of this exception, if available.
         *
         * @return the Throwable that was the root cause of this exception,
         * which may be <code>null</code>.
         */
        public Throwable getCause() {
            return nativeException.getCause();
        }

        /**
         * Returns the detail message for this exception.
         *
         * @return the detail message, which may be <code>null</code>.
         */
        public String getMessage() {
            return nativeException.getMessage();
        }

        /**
         * Returns a <code>String</code> representation of this exception.
         *
         * @return a <code>String</code> representation of this exception.
         */
        public String toString() {
            return nativeException.toString();
        }
    }


    public static MqttException asUncheckMqttException(org.eclipse.paho.client.mqttv3.MqttException ex) {
        return new MqttException(ex);
    }
}
