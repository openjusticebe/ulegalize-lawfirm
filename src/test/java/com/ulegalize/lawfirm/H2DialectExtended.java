package com.ulegalize.lawfirm;

import org.hibernate.dialect.H2Dialect;

public class H2DialectExtended extends H2Dialect {

    @Override
    public String toBooleanValueString(boolean bool) {
        return bool ? "1" : "0";
    }

}
