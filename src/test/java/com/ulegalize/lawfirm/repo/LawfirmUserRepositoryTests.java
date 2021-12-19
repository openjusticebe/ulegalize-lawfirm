package com.ulegalize.lawfirm.repo;

import com.ulegalize.lawfirm.EntityTest;
import com.ulegalize.lawfirm.model.entity.LawfirmEntity;
import com.ulegalize.lawfirm.model.entity.LawfirmUsers;
import com.ulegalize.lawfirm.repository.LawfirmUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
@Slf4j
public class LawfirmUserRepositoryTests extends EntityTest {
    @Autowired
    private LawfirmUserRepository lawfirmUserRepository;

    @Test
    public void test_A_find() {
        LawfirmEntity lawfirm = createLawfirm();
        createLawfirmWebsiteEntity(lawfirm);

        List<LawfirmUsers> lawfirmDTOS = lawfirmUserRepository.findLawfirmUsersByUserId(lawfirm.getLawfirmUsers().get(0).getUser().getId());
        assertNotNull(lawfirmDTOS);

        assertEquals(1, lawfirmDTOS.size());
        assertEquals(lawfirm.getVckey(), lawfirmDTOS.get(0).getLawfirm().getVckey());

    }

}
