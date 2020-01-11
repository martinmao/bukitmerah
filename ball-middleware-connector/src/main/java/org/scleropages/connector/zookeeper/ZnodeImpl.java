package org.scleropages.connector.zookeeper;

import org.apache.zookeeper.data.Stat;

public class ZnodeImpl implements ZNode {

        private final String path;

        private final Stat stat;

        private final byte[] data;

        public ZnodeImpl(String path, Stat stat, byte[] data) {
            this.path = path;
            this.stat = stat;
            this.data = data;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public Stat stat() {
            return null;
        }

        @Override
        public byte[] data() {
            return new byte[0];
        }


}