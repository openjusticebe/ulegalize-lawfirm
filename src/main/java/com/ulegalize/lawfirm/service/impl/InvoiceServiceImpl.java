package com.ulegalize.lawfirm.service.impl;

import com.ulegalize.dto.*;
import com.ulegalize.lawfirm.model.LawfirmToken;
import com.ulegalize.lawfirm.model.converter.DTOToInvoiceDetailsEntityConverter;
import com.ulegalize.lawfirm.model.converter.DTOToInvoiceEntityConverter;
import com.ulegalize.lawfirm.model.converter.EntityToInvoiceConverter;
import com.ulegalize.lawfirm.model.entity.*;
import com.ulegalize.lawfirm.model.enumeration.EnumFactureType;
import com.ulegalize.lawfirm.repository.*;
import com.ulegalize.lawfirm.rest.DriveFactory;
import com.ulegalize.lawfirm.rest.v2.DriveApi;
import com.ulegalize.lawfirm.rest.v2.ReportApi;
import com.ulegalize.lawfirm.service.InvoiceService;
import com.ulegalize.lawfirm.service.SearchService;
import com.ulegalize.lawfirm.utils.DriveUtils;
import com.ulegalize.lawfirm.utils.InvoicesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {
    private final TFacturesRepository facturesRepository;
    private final LawfirmRepository lawfirmRepository;
    private final RefPosteRepository refPosteRepository;
    private final LawfirmUserRepository lawfirmUserRepository;
    private final TTimesheetRepository timesheetRepository;
    private final SearchService searchService;
    private final EntityToInvoiceConverter entityToInvoiceConverter;
    private final DTOToInvoiceEntityConverter dtoToInvoiceEntityConverter;
    private final DTOToInvoiceDetailsEntityConverter dtoToInvoiceDetailsEntityConverter;
    private final DossierRepository dossierRepository;
    private final ClientRepository clientRepository;
    private final TFactureEcheanceRepository tfactureEcheanceRepository;
    private final ReportApi reportApi;
    private final DriveFactory driveFactory;
    @Value("${spring.profiles.active}")
    private String activeProfile;

    public InvoiceServiceImpl(TFacturesRepository facturesRepository,
                              LawfirmRepository lawfirmRepository,
                              RefPosteRepository refPosteRepository,
                              LawfirmUserRepository lawfirmUserRepository,
                              TTimesheetRepository timesheetRepository,
                              SearchService searchService,
                              EntityToInvoiceConverter entityToInvoiceConverter,
                              DTOToInvoiceEntityConverter dtoToInvoiceEntityConverter,
                              DTOToInvoiceDetailsEntityConverter dtoToInvoiceDetailsEntityConverter,
                              DossierRepository dossierRepository,
                              ClientRepository clientRepository,
                              TFactureEcheanceRepository tfactureEcheanceRepository, ReportApi reportApi,
                              DriveFactory driveFactory) {
        this.facturesRepository = facturesRepository;
        this.lawfirmRepository = lawfirmRepository;
        this.entityToInvoiceConverter = entityToInvoiceConverter;
        this.refPosteRepository = refPosteRepository;
        this.lawfirmUserRepository = lawfirmUserRepository;
        this.timesheetRepository = timesheetRepository;
        this.searchService = searchService;
        this.dtoToInvoiceEntityConverter = dtoToInvoiceEntityConverter;
        this.dtoToInvoiceDetailsEntityConverter = dtoToInvoiceDetailsEntityConverter;
        this.dossierRepository = dossierRepository;
        this.clientRepository = clientRepository;
        this.tfactureEcheanceRepository = tfactureEcheanceRepository;
        this.reportApi = reportApi;
        this.driveFactory = driveFactory;
    }

    @Override
    public Page<InvoiceDTO> getAllInvoices(int limit, int offset, String vcKey, Integer searchEcheance,
                                           ZonedDateTime searchDate, String searchYearDossier,
                                           Long searchNumberDossier, String searchClient) {
        log.debug("Get all Invoices with user {} limit {} and offset {}", vcKey, limit, offset);

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "idFactureType");
        Sort.Order order2 = new Sort.Order(Sort.Direction.DESC, "factureRef");
        Pageable pageable = new OffsetBasedPageRequest(limit, offset, Sort.by(order, order2));
        // if it's 0 , transform to null like this it will query on all result
        Long number = searchNumberDossier != null && searchNumberDossier == 0 ? null : searchNumberDossier;

        Page<TFactures> allInvoices;
        if (searchDate != null) {
            allInvoices = facturesRepository.findAllByDateWithPagination(vcKey, searchEcheance, searchDate, searchYearDossier, number, pageable);
        } else {
            allInvoices = facturesRepository.findAllWithPagination(vcKey, searchEcheance, searchYearDossier, number, searchClient, pageable);

        }
        List<InvoiceDTO> invoiceDTOList = !CollectionUtils.isEmpty(allInvoices.getContent()) ? entityToInvoiceConverter.convertToList(allInvoices.getContent()) : new ArrayList<>();

        return new PageImpl<>(invoiceDTOList, Pageable.unpaged(), allInvoices.getTotalElements());
    }

    @Override
    public Page<InvoiceDTO> getAllInvoicesByDossierId(int limit, int offset, Long dossierId, String vcKey) {
        log.debug("Get all Invoices with user {} limit {} and offset {} and dossierId {}", vcKey, limit, offset, dossierId);

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "idFactureType");
        Sort.Order order2 = new Sort.Order(Sort.Direction.DESC, "factureRef");
        Pageable pageable = new OffsetBasedPageRequest(limit, offset, Sort.by(order, order2));
        Page<TFactures> allInvoices = facturesRepository.findByDossierIdWithPagination(dossierId, vcKey, pageable);
        List<InvoiceDTO> invoiceDTOList = !CollectionUtils.isEmpty(allInvoices.getContent()) ? entityToInvoiceConverter.convertToList(allInvoices.getContent()) : new ArrayList<>();

        return new PageImpl<>(invoiceDTOList, Pageable.unpaged(), allInvoices.getTotalElements());
    }

    @Override
    public List<ItemLongDto> getInvoicesBySearchCriteria(String vcKey, String searchCriteria) {

        log.debug("Get all Invoices with user {} ", vcKey);
        List<TFactures> allInvoices = facturesRepository.findAll(vcKey);
        List<InvoiceDTO> invoiceDTOList = entityToInvoiceConverter.convertToList(allInvoices);

        List<InvoiceDTO> resultList = invoiceDTOList;
        if (searchCriteria != null && !searchCriteria.isEmpty()) {

            resultList = invoiceDTOList.stream()
                    .filter(facture
                            -> facture.getReference().toLowerCase().contains(searchCriteria.toLowerCase()))
                    .collect(toList());

        }
        return resultList.stream()
                .map(facture -> new ItemLongDto(facture.getId(), facture.getReference()))
                .collect(toList());
    }

    @Override
    public InvoiceDTO getDefaultInvoice(Long userId, String vcKey) {

        log.debug("Entering getDefaultInvoice with user id {} and vckey {}", userId, vcKey);

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setVcKey(vcKey);
        invoiceDTO.setValid(false);

        // facture type must be an enum
        invoiceDTO.setTypeId(EnumFactureType.TEMP.getId());
        invoiceDTO.setTypeItem(new ItemLongDto(
                EnumFactureType.TEMP.getId(),
                EnumFactureType.TEMP.getDescription()
        ));

        invoiceDTO.setMontant(BigDecimal.ZERO);

        Optional<RefPoste> refPoste = refPosteRepository.findFirstByVcKeyAndHonoraires(vcKey, true);

        refPoste.ifPresent(poste -> {
            invoiceDTO.setPosteId(poste.getIdPoste());
            invoiceDTO.setPosteItem(new ItemDto(poste.getIdPoste(), poste.getRefPoste()));
        });

        List<ItemDto> tFactureEcheance = searchService.getFactureEcheances(vcKey);
        if (!tFactureEcheance.isEmpty()) {
            invoiceDTO.setEcheanceId(tFactureEcheance.get(0).getValue());
            invoiceDTO.setEcheanceItem(tFactureEcheance.get(0));
        }
        invoiceDTO.setDateValue(ZonedDateTime.now());

        invoiceDTO.setDateEcheance(invoiceDTO.getDateValue().plusDays(7));

        List<ItemVatDTO> vats = searchService.getVats(vcKey);

        int maxIndexVat = vats != null && !vats.isEmpty() ? vats.size() - 1 : 0;
        invoiceDTO.setInvoiceDetailsDTOList(new ArrayList<>());

        invoiceDTO.getInvoiceDetailsDTOList().add(new InvoiceDetailsDTO(
                null, null, "",
                vats.get(maxIndexVat).getValue(),
                vats.get(maxIndexVat), BigDecimal.ZERO,
                BigDecimal.ZERO
        ));

        log.debug("Leaving getDefaultInvoice with user id {} and vckey {}", userId, vcKey);

        return invoiceDTO;
    }

    @Override
    public InvoiceDTO getInvoiceById(Long invoiceId, String vcKey) {
        log.debug("Entering getInvoiceById with invoiceId {} ", invoiceId);
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        InvoiceDTO invoiceDTO = null;
        Optional<TFactures> facturesOptional = facturesRepository.findByIdFactureAndVcKey(invoiceId, vcKey);

        if (!facturesOptional.isPresent()) {
            log.warn("invoice is not existing invoiceId {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice is not existing");
        } else {

            invoiceDTO = entityToInvoiceConverter.apply(facturesOptional.get());

            List<ItemVatDTO> vats = searchService.getVats(facturesOptional.get().getVcKey());

            // facture details
            if (facturesOptional.get().getTFactureDetailsList() != null && !facturesOptional.get().getTFactureDetailsList().isEmpty()) {

                for (TFactureDetails factureDetails : facturesOptional.get().getTFactureDetailsList()) {
                    List<ItemVatDTO> itemBigDecimalDto = vats.stream().filter(vat -> vat.getValue().compareTo(factureDetails.getTva()) == 0).collect(toList());

                    InvoiceDetailsDTO invoiceDetailsDTO = new InvoiceDetailsDTO(
                            factureDetails.getId(),
                            factureDetails.getTFactures().getIdFacture(),
                            factureDetails.getDescription(),
                            factureDetails.getTva(),
                            itemBigDecimalDto.get(0),
                            factureDetails.getTtc(),
                            factureDetails.getHtva()
                    );
                    invoiceDTO.getInvoiceDetailsDTOList().add(invoiceDetailsDTO);
                }
            } else {

                int maxIndexVat = vats != null && !vats.isEmpty() ? vats.size() - 1 : 0;
                invoiceDTO.setInvoiceDetailsDTOList(new ArrayList<>());

                invoiceDTO.getInvoiceDetailsDTOList().add(new InvoiceDetailsDTO(
                        null, null, "",
                        vats.get(maxIndexVat).getValue(),
                        vats.get(maxIndexVat), BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }

            // prestation
            if (facturesOptional.get().getTFactureTimesheetList() != null && !facturesOptional.get().getTFactureTimesheetList().isEmpty()) {

                for (TFactureTimesheet tFactureTimesheet : facturesOptional.get().getTFactureTimesheetList()) {
                    invoiceDTO.getPrestationIdList().add(tFactureTimesheet.getTsId());
                }
            }
        }
        log.debug("Leaving getInvoiceById with invoiceId {} ", invoiceId);
        return invoiceDTO;
    }

    @Override
    public Long createInvoice(InvoiceDTO invoiceDTO, String vcKey) {
        log.debug("Entering createInvoice with {}", invoiceDTO.toString());
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (invoiceDTO.getInvoiceDetailsDTOList() == null || invoiceDTO.getInvoiceDetailsDTOList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "facture details cannot be empty");
        }
        invoiceDTO.setVcKey(vcKey);
        Integer yearFacture = invoiceDTO.getDateValue().getYear();
        Integer numFacture = facturesRepository.getMaxNumFactTempByVcKey(vcKey);
        Optional<LawfirmDTO> lawfirmDTOOptional = lawfirmRepository.findLawfirmDTOByVckey(vcKey);

        if (!lawfirmDTOOptional.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "lawfirm is not found");
        }

        if (numFacture == null) {
            numFacture = lawfirmDTOOptional.get().getStartInvoiceNumber();
        } else {
            numFacture++;
        }
        EnumFactureType enumFactureType = EnumFactureType.fromId(invoiceDTO.getTypeId());
        if (enumFactureType == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture type is not found");
        }

        String factureRef = InvoicesUtils.getInvoiceReference(yearFacture, numFacture, enumFactureType);
        invoiceDTO.setYearFacture(yearFacture);
        invoiceDTO.setNumFacture(numFacture);
        invoiceDTO.setReference(factureRef);

        TFactures tFactures = dtoToInvoiceEntityConverter.apply(invoiceDTO, new TFactures());

        log.info("Create facture details");
        invoiceDTO.getInvoiceDetailsDTOList().forEach(invoiceDetailsDTO -> {
            TFactureDetails tFactureDetails = dtoToInvoiceDetailsEntityConverter.apply(invoiceDetailsDTO, new TFactureDetails());
            tFactureDetails.setUserUpd(lawfirmToken.getUsername());
            tFactureDetails.setDateUpd(LocalDateTime.now());
            tFactures.addTFactureDetails(tFactureDetails);
            log.info("invoiceDetailsDTO : {} was added to tFacture : {} ", invoiceDetailsDTO, tFactures);
        });

        invoiceDTO.getPrestationIdList().forEach(prestationId -> {
            TFactureTimesheet tFactureTimesheet = new TFactureTimesheet();
            tFactureTimesheet.setTsId(prestationId);
            tFactureTimesheet.setCreUser(lawfirmToken.getUsername());
            tFactureTimesheet.setUpdUser(lawfirmToken.getUsername());
            tFactures.addTFactureTimesheet(tFactureTimesheet);
            log.info("TFactureTimesheet : {} was added to tFacture : {} ", prestationId, tFactures);
        });

        tFactures.setDateUpd(LocalDateTime.now());
        tFactures.setUserUpd(lawfirmToken.getUsername());

        facturesRepository.save(tFactures);

        log.info("invoice saved in repo with id {}: ", tFactures.getIdFacture());
        log.debug("Leaving createInvoice with {}", invoiceDTO.toString());
        return tFactures.getIdFacture();
    }

    @Override
    public InvoiceDTO updateInvoice(InvoiceDTO invoiceDTO, String vcKey) {
        log.debug("Entering updateInvoice with invoice id : {}", invoiceDTO.getId());
        commonRules(invoiceDTO);

        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (invoiceDTO.getId() == null) {
            log.warn("invoice is not filled in {}", invoiceDTO.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice is not filled in");
        }

        Optional<TFactures> tFacturesOptional = facturesRepository.findByIdFactureAndVcKey(invoiceDTO.getId(), vcKey);

        if (!tFacturesOptional.isPresent()) {
            log.warn("invoice does not exist {}", invoiceDTO.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice does not exist");
        }

        if (tFacturesOptional.get().getValid()) {
            log.warn("invoice cannot be modified {}", invoiceDTO.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice cannot be modified");
        }

        if (invoiceDTO.getInvoiceDetailsDTOList() != null) {

            TFactureDetails factureDetails;
            boolean factureDetailsExist;

            List<TFactureDetails> difference = new ArrayList<>();
            boolean exist = false;
            for (TFactureDetails tFactureDetails : tFacturesOptional.get().getTFactureDetailsList()) {
                for (InvoiceDetailsDTO invoiceDetailsDTO : invoiceDTO.getInvoiceDetailsDTOList()) {
                    if (tFactureDetails.getId().equals(invoiceDetailsDTO.getId())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    difference.add(tFactureDetails);
                }
                exist = false;
            }
            tFacturesOptional.get().getTFactureDetailsList().removeAll(difference);

            for (InvoiceDetailsDTO invoiceDetailsDTO : invoiceDTO.getInvoiceDetailsDTOList()) {
                factureDetailsExist = false;
                for (TFactureDetails factureDetailsExisting : tFacturesOptional.get().getTFactureDetailsList()) {
                    if (invoiceDetailsDTO.getId() != null && factureDetailsExisting.getId().equals(invoiceDetailsDTO.getId())) {
                        factureDetailsExist = true;
                        factureDetailsExisting.setTFactures(tFacturesOptional.get());
                        factureDetailsExisting.setDescription(invoiceDetailsDTO.getDescription());
                        factureDetailsExisting.setHtva(invoiceDetailsDTO.getMontantHt());
                        factureDetailsExisting.setTtc(invoiceDetailsDTO.getMontant());
                        factureDetailsExisting.setTva(invoiceDetailsDTO.getTva());
                        factureDetailsExisting.setDateUpd(LocalDateTime.now());
                        factureDetailsExisting.setUserUpd(lawfirmToken.getUsername());
                    }
                }
                if (!factureDetailsExist) {
                    factureDetails = new TFactureDetails();
                    factureDetails.setDescription(invoiceDetailsDTO.getDescription());
                    factureDetails.setHtva(invoiceDetailsDTO.getMontantHt());
                    factureDetails.setTtc(invoiceDetailsDTO.getMontant());
                    factureDetails.setTva(invoiceDetailsDTO.getTva());
                    factureDetails.setDateUpd(LocalDateTime.now());
                    factureDetails.setUserUpd(lawfirmToken.getUsername());
                    tFacturesOptional.get().addTFactureDetails(factureDetails);
                }
            }
        } else {
            log.warn("InvoiceDetailsList is empty {}", invoiceDTO.getInvoiceDetailsDTOList());
            tFacturesOptional.get().getTFactureDetailsList().clear();
        }


        if (invoiceDTO.getPrestationIdList() != null) {
            List<TFactureTimesheet> tFactureTimesheetList = new ArrayList<>();
            tFacturesOptional.get().getTFactureTimesheetList().clear();
            facturesRepository.save(tFacturesOptional.get());

            for (Long prestattionId : invoiceDTO.getPrestationIdList()) {
                TFactureTimesheet tFactureTimesheet = new TFactureTimesheet();
                tFactureTimesheet.setTsId(prestattionId);
                tFactureTimesheet.setCreUser(lawfirmToken.getUsername());
                tFactureTimesheetList.add(tFactureTimesheet);
                tFactureTimesheet.setTFactures(tFacturesOptional.get());
            }
            tFacturesOptional.get().getTFactureTimesheetList().addAll(tFactureTimesheetList);

        } else {
            log.warn("PrestationIdList is empty {}", invoiceDTO.getPrestationIdList());
            tFacturesOptional.get().getTFactureTimesheetList().clear();
        }

        tFacturesOptional.get().setDateValue(invoiceDTO.getDateValue());
        tFacturesOptional.get().setDateEcheance(invoiceDTO.getDateEcheance());

        if (invoiceDTO.getEcheanceId() != null) {
            Optional<TFactureEcheance> factureEcheance = tfactureEcheanceRepository.findById(invoiceDTO.getEcheanceId());

            factureEcheance.ifPresent(tFactureEcheance -> tFacturesOptional.get().setIdEcheance(invoiceDTO.getEcheanceId()));
        }
        EnumFactureType enumFactureType = EnumFactureType.fromId(invoiceDTO.getTypeId());
        if (enumFactureType == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice type cannot be nuull");
        }
        tFacturesOptional.get().setIdFactureType(enumFactureType);

        if (invoiceDTO.getClientId() != null) {
            Optional<TClients> tClientsOptional = clientRepository.findById(invoiceDTO.getClientId());

            tClientsOptional.ifPresent(tclient -> tFacturesOptional.get().setIdTiers(invoiceDTO.getClientId()));
        }
        if (invoiceDTO.getDossierId() != null) {
            Optional<TDossiers> tDossiersOptional = dossierRepository.findById(invoiceDTO.getDossierId());

            tDossiersOptional.ifPresent(dossiers -> tFacturesOptional.get().setIdDoss(invoiceDTO.getDossierId()));
        }
        facturesRepository.save(tFacturesOptional.get());

        log.debug("Leaving updateInvoice with invoice id : {}", invoiceDTO.getId());
        return invoiceDTO;
    }

    public void commonRules(InvoiceDTO invoiceDTO) {
        if (invoiceDTO == null) {
            log.warn("Invoice is not filled in");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invoice is not filled in");
        }

        if (invoiceDTO.getDossierId() != null) {
            Optional<TDossiers> tDossiers = dossierRepository.findById(invoiceDTO.getDossierId());
            if (!tDossiers.isPresent()) {
                log.warn("Dossier is not found {} ", invoiceDTO.getDossierId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier is not found");
            }
        }

        if (invoiceDTO.getClientId() != null) {
            Optional<TClients> tClients = clientRepository.findById(invoiceDTO.getClientId());
            if (!tClients.isPresent()) {
                log.warn("CLient is not found {} ", invoiceDTO.getClientId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client is not found");
            }
        }

        Optional<RefPoste> refPoste = refPosteRepository.findById(invoiceDTO.getPosteId());
        if (!refPoste.isPresent()) {
            log.warn("Post is not found {} ", invoiceDTO.getPosteId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post is not found");
        }

        if (invoiceDTO.getEcheanceId() != null) {
            Optional<TFactureEcheance> tFactureEcheanceOptional = tfactureEcheanceRepository.findById(invoiceDTO.getEcheanceId());
            if (!tFactureEcheanceOptional.isPresent()) {
                log.warn("Facture Echeance is not found {}", invoiceDTO.getEcheanceId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture Echeance is not found");
            }
        }

        Optional<EnumFactureType> enumFactureType = Optional.ofNullable(EnumFactureType.fromId(invoiceDTO.getTypeId()));
        if (!enumFactureType.isPresent()) {
            log.warn("FactureType is not found {} ", invoiceDTO.getTypeId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FactureType is not found");
        }

    }

    @Override
    public List<PrestationSummary> getPrestationByDossierId(Long invoiceId, Long dossierId, Long userId, String vcKey) {
        log.debug("getPrestationByDossierId with user {} and dossierId {}", userId, dossierId);
        Optional<LawfirmUsers> lawfirmUsers = lawfirmUserRepository.findLawfirmUsersByVcKeyAndUserId(vcKey, userId);

        if (lawfirmUsers.isPresent()) {
            log.debug("Law firm list {} user id {}", lawfirmUsers.get().getId(), userId);

            return timesheetRepository.findAllByInvoiceIdDossierId(invoiceId, dossierId, lawfirmUsers.get().getId());
        }

        return new ArrayList<>();


    }

    @Override
    public Long countInvoiceDetailsByVat(BigDecimal vat) {
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Entering countInvoiceDetailsByVat {} and vckey {}", vat, lawfirmToken.getVcKey());

        // chek if it's used in t_factures
        Long vatNb = facturesRepository.countAllByVcKeyAndVat(lawfirmToken.getVcKey(), vat);
        log.debug("Number of invoices {} found in the vckey {}", vatNb, lawfirmToken.getVcKey());

        return vatNb;
    }

    @Override
    public InvoiceDTO validateInvoice(Long invoiceId, String vcKey) {
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.debug("Entering validateInvoice implementation with invoiceId = {} and vcKey {}", invoiceId, lawfirmToken.getVcKey());
        InvoiceDTO invoiceDTO = null;
        if (invoiceId == null) {
            log.warn("invoiceId is null {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoiceId is null");
        }

        Optional<TFactures> tFacturesOptional = facturesRepository.findByIdFactureAndVcKey(invoiceId, vcKey);

        if (!tFacturesOptional.isPresent()) {
            log.warn("invoice does not exist {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice does not exist");
        }

        if (tFacturesOptional.get().getValid()) {
            log.warn("invoice is already valid {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice is already valid");
        } else {
            if (tFacturesOptional.get().getIdFactureType().getId() == 3) {

                tFacturesOptional.get().setIdFactureType(EnumFactureType.SELL);

                Integer yearFacture = tFacturesOptional.get().getDateValue().getYear();
                Integer numFacture = facturesRepository.getMaxNumFacture(vcKey, tFacturesOptional.get().getIdFactureType(), yearFacture);
                Optional<LawfirmDTO> lawfirmDTOOptional = lawfirmRepository.findLawfirmDTOByVckey(vcKey);

                if (!lawfirmDTOOptional.isPresent()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "lawfirm is not found");
                }

                if (numFacture == null) {
                    numFacture = lawfirmDTOOptional.get().getStartInvoiceNumber();
                } else {
                    numFacture++;
                }
                tFacturesOptional.get().setNumFacture(numFacture);

                String factureRef = InvoicesUtils.getInvoiceReference(yearFacture, tFacturesOptional.get().getNumFacture(), tFacturesOptional.get().getIdFactureType());
                tFacturesOptional.get().setFactureRef(factureRef);
            }

            tFacturesOptional.get().setNumFactTemp(0);
            tFacturesOptional.get().setValid(true);
        }

        facturesRepository.save(tFacturesOptional.get());

        invoiceDTO = getInvoiceById(invoiceId, vcKey);

        sendInvoiceToDrive(lawfirmToken, invoiceId, tFacturesOptional.get().getFactureRef(), tFacturesOptional.get().getYearFacture());

        log.debug("Leaving validateInvoice implementation with isValid = {}", invoiceDTO.getValid());

        return invoiceDTO;
    }

    @Override
    public ByteArrayResource downloadInvoice(Long invoiceId) {
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.debug("Entering validateInvoice implementation with invoiceId = {} and vcKey {}", invoiceId, lawfirmToken.getVcKey());
        if (invoiceId == null) {
            log.warn("invoiceId is null {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoiceId is null");
        }

        Optional<TFactures> tFacturesOptional = facturesRepository.findByIdFactureAndVcKey(invoiceId, lawfirmToken.getVcKey());

        if (!tFacturesOptional.isPresent()) {
            log.warn("invoice does not exist {}", invoiceId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invoice does not exist");
        }
        DriveApi driveApi = driveFactory.getDriveImpl(lawfirmToken.getDriveType());
        return driveApi.downloadFile(lawfirmToken, DriveUtils.INVOICE_PATH + tFacturesOptional.get().getYearFacture() + "/" + tFacturesOptional.get().getFactureRef() + ".pdf");
    }

    @Override
    public Long deleteInvoiceById(Long invoiceId) {
        LawfirmToken lawfirmToken = (LawfirmToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Optional<TFactures> tFacturesOptional = facturesRepository.findByIdFactureAndVcKey(invoiceId, lawfirmToken.getVcKey());
        if (!tFacturesOptional.isPresent()) {
            log.warn("invoice is not found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invoice is not found");
        }
        facturesRepository.delete(tFacturesOptional.get());
        return invoiceId;
    }

    private void sendInvoiceToDrive(LawfirmToken lawfirmToken, Long invoiceId, String factureRef, Integer yearFacture) {
        log.debug("Entering sendInvoiceToDrive invoiceId = {} for facture ref = {}", invoiceId, factureRef);
        if (!activeProfile.equalsIgnoreCase("integrationtest")
                && !activeProfile.equalsIgnoreCase("dev")
                && !activeProfile.equalsIgnoreCase("devDocker")) {
            // get invoice pdf
            ByteArrayResource resourceInvoice = reportApi.getInvoice(lawfirmToken, invoiceId);

            log.debug("Getting invoice pdf factureRef = {} ", factureRef);
            DriveApi driveApi = driveFactory.getDriveImpl(lawfirmToken.getDriveType());
            // send it to Udrive
            driveApi.uploadFile(lawfirmToken, resourceInvoice.getByteArray(), factureRef + ".pdf", DriveUtils.INVOICE_PATH + yearFacture + "/");
            log.debug("Leaving sendInvoiceToDrive with factureRef = {}", factureRef);
        }
    }
}
