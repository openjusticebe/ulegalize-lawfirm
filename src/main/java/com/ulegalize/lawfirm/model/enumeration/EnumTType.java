package com.ulegalize.lawfirm.model.enumeration;

import lombok.Getter;

public enum EnumTType {
    SORTIE(1, "Sortie", "Output", "Uitgang"),
    ENTREE(2, "Entrée", "Entry", "Ingang");

    @Getter
    private Integer idType;
    @Getter
    private String descriptionFr;
    @Getter
    private String descriptionEn;
    @Getter
    private String descriptionNl;

    EnumTType(Integer id, String descriptionFr, String descriptionEn, String descriptionNl) {
        this.idType = id;
        this.descriptionFr = descriptionFr;
        this.descriptionEn = descriptionEn;
        this.descriptionNl = descriptionNl;
    }

    public static EnumTType fromId(Integer id) {
        for (EnumTType enumTType : values()) {
            if (enumTType.getIdType().equals(id)) {
                return enumTType;
            }
        }
        return null;
    }
}
