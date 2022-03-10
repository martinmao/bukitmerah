package org.scleropages.crud.uid.provider.leaf;

import org.scleropages.crud.uid.provider.leaf.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
