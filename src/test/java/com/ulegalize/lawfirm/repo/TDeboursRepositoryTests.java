package com.ulegalize.lawfirm.repo;

import com.ulegalize.dto.FraisAdminDTO;
import com.ulegalize.enumeration.EnumVCOwner;
import com.ulegalize.lawfirm.EntityTest;
import com.ulegalize.lawfirm.model.entity.LawfirmEntity;
import com.ulegalize.lawfirm.model.entity.TDebour;
import com.ulegalize.lawfirm.model.entity.TDossiers;
import com.ulegalize.lawfirm.repository.TDebourRepository;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class TDeboursRepositoryTests extends EntityTest {
    @Autowired
    private TDebourRepository tDebourRepository;

    @Test
    public void test_A_sumByDossierId() {
        LawfirmEntity lawfirm = createLawfirm();

        TDossiers dossier = createDossier(lawfirm, EnumVCOwner.OWNER_VC);
        TDebour tDebour = createTDebour(lawfirm, dossier);

        BigDecimal countAllJusticeByVcKey = tDebourRepository.sumByDossierId(tDebour.getIdDoss(), lawfirm.getLawfirmUsers().get(0).getId());

        assertNotNull(countAllJusticeByVcKey);
        BigDecimal result = tDebour.getPricePerUnit().multiply(BigDecimal.valueOf(tDebour.getUnit()));
        assertThat(result, Matchers.comparesEqualTo(countAllJusticeByVcKey));
    }

    @Test
    public void test_B_countAllByIdAndDossierId() {
        LawfirmEntity lawfirm = createLawfirm();

        TDossiers dossier = createDossier(lawfirm, EnumVCOwner.OWNER_VC);
        TDebour tDebour = createTDebour(lawfirm, dossier);

        Long countAllJusticeByVcKey = tDebourRepository.countAllByIdAndDossierId(List.of(tDebour.getIdDebour()), tDebour.getIdDoss(), lawfirm.getLawfirmUsers().get(0).getId());

        assertNotNull(countAllJusticeByVcKey);
        assertEquals(1, countAllJusticeByVcKey.intValue());
    }

    @Test
    public void test_B1_countAllByIdAndDossierId_zero() {
        LawfirmEntity lawfirm = createLawfirm();

        TDossiers dossier = createDossier(lawfirm, EnumVCOwner.OWNER_VC);
        TDossiers dossier2 = createDossier(lawfirm, EnumVCOwner.OWNER_VC);
        TDebour tDebour = createTDebour(lawfirm, dossier);


        Long countAllJusticeByVcKey = tDebourRepository.countAllByIdAndDossierId(List.of(tDebour.getIdDebour()), dossier2.getIdDoss(), lawfirm.getLawfirmUsers().get(0).getId());

        assertNotNull(countAllJusticeByVcKey);
        assertEquals(0, countAllJusticeByVcKey.intValue());
    }

    @Test
    public void test_C_findAllByInvoiceIdDossierId() {
        LawfirmEntity lawfirm = createLawfirm();

        TDossiers dossier = createDossier(lawfirm, EnumVCOwner.OWNER_VC);
        TDebour tDebour = createTDebour(lawfirm, dossier);

        List<FraisAdminDTO> fraisAdminDTOList = tDebourRepository.findAllByInvoiceIdDossierId(tDebour.getIdDebour(), tDebour.getIdDoss(), lawfirm.getLawfirmUsers().get(0).getId());

        assertNotNull(fraisAdminDTOList);
        assertEquals(1, fraisAdminDTOList.size());
    }
}
