package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.domain.model.*;
import com.montran.creditscore.domain.port.outbound.UserStore;
import com.montran.creditscore.infrastructure.persistence.dto.CreditHistoryStorageDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public abstract class AbstractFileUserStore implements UserStore {

    /** @docs Thread-safe internal memory cache mapping SSN to User Aggregates. */
    protected final ConcurrentMap<String, User> cache = new ConcurrentHashMap<>();

    /**
     * @docs High-performance locking mechanism for read-heavy, write-isolated
     *       concurrency control.
     */
    protected final StampedLock lock = new StampedLock();

    /**
     * @docs Standard ISO-8601 thread-local date parser (SimpleDateFormat is not
     *       thread-safe, so we instantiate locally or synchronize usage).
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * @docs Base constructor that triggers the initial file loading sequence upon
     *       instantiation.
     */
    protected AbstractFileUserStore() {
        loadStateIntoCache();
    }

    /**
     * @docs Template method requiring subclass to execute physical file reads.
     * @return List of parsed UserStorageDto elements from the physical file.
     */
    protected abstract List<UserStorageDto> readFromFile();

    /**
     * @docs Template method requiring subclass to execute physical file writes.
     * @param dtos The full system state translated into DTOs ready for writing.
     */
    protected abstract void writeToFile(List<UserStorageDto> dtos);

    @Override
    public Optional<User> findBySsn(String ssn) {
        long stamp = lock.tryOptimisticRead();
        User user = cache.get(ssn);
        // If a write occurred during our optimistic read, fall back to a strict read
        // lock.
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                user = cache.get(ssn);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Optional.ofNullable(user);
    }

    @Override
    public List<User> findAll() {
        long stamp = lock.readLock();
        try {
            return new ArrayList<>(cache.values());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void save(User user) {
        long stamp = lock.writeLock();
        try {
            cache.put(user.getSsn(), user);
            flushCacheToFile();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean deleteBySsn(String ssn) {
        long stamp = lock.writeLock();
        try {
            if (cache.remove(ssn) != null) {
                flushCacheToFile();
                return true;
            }
            return false;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * @docs Translates the entire current cache state into flat DTOs
     *       and delegates the disk write.
     */
    private void flushCacheToFile() {
        List<UserStorageDto> dtos = cache.values().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        writeToFile(dtos);
    }

    /**
     * @docs Bootstraps the memory cache from the disk structure safely on startup.
     */
    private void loadStateIntoCache() {
        long stamp = lock.writeLock();
        try {
            List<UserStorageDto> dtos = readFromFile();
            cache.clear();
            if (dtos != null) {
                for (UserStorageDto dto : dtos) {
                    User domainUser = mapToDomain(dto);
                    cache.put(domainUser.getSsn(), domainUser);
                }
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * @docs Maps a pure Domain Aggregate to a structural DTO.
     */
    private UserStorageDto mapToDto(User user) {
        UserStorageDto dto = new UserStorageDto();
        dto.setSsn(user.getSsn());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        dto.setEmail(user.getEmail());
        dto.setCreditScore(user.getCreditScore());
        dto.setRiskLevel(user.getRiskLevel().name());
        dto.setTotalCreditLimit(user.getTotalCreditLimit());

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        // CORRECCIÓN: La lista debe inicializarse antes del bucle para poder almacenar
        // los datos.
        List<CreditHistoryStorageDto> historyDtos = new ArrayList<>();

        for (CreditHistoryRecord record : user.getCreditHistory()) {
            CreditHistoryStorageDto recDto = new CreditHistoryStorageDto();

            // Mapeo seguro de fechas
            recDto.setDueDateStr(sdf.format(record.getDueDate()));
            if (record.getSettlementDate() != null) {
                recDto.setSettlementDateStr(sdf.format(record.getSettlementDate()));
            }

            recDto.setTransactionType(record.getTransactionType().name());
            recDto.setAmount(record.getAmount());
            recDto.setStatus(record.getStatus().name());

            // Ahora la lista existe y puede recibir los elementos en cada iteración
            historyDtos.add(recDto);
        }

        dto.setCreditHistory(historyDtos);
        return dto;
    }

    /**
     * @docs Maps a structural DTO back into a pure Domain Aggregate.
     */
    private User mapToDomain(UserStorageDto dto) {
        User user = new User(dto.getSsn(), dto.getName(), dto.getAddress(), dto.getEmail(), dto.getTotalCreditLimit());
        user.setCreditScore(dto.getCreditScore());
        user.setRiskLevel(RiskLevel.valueOf(dto.getRiskLevel()));

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (dto.getCreditHistory() != null) {
            for (CreditHistoryStorageDto recDto : dto.getCreditHistory()) {
                try {
                    // Evaluación de nulos para la fecha de pago
                    Date dueDate = sdf.parse(recDto.getDueDateStr());
                    Date settlementDate = null;

                    if (recDto.getSettlementDateStr() != null && !recDto.getSettlementDateStr().isEmpty()) {
                        settlementDate = sdf.parse(recDto.getSettlementDateStr());
                    }

                    CreditHistoryRecord record = new CreditHistoryRecord(
                            dueDate,
                            settlementDate,
                            TransactionType.valueOf(recDto.getTransactionType()),
                            recDto.getAmount(),
                            TransactionStatus.valueOf(recDto.getStatus()));
                    user.addCreditRecord(record);
                } catch (ParseException e) {
                    throw new IllegalStateException("Corrupted date format in persistence file.", e);
                }
            }
        }
        return user;
    }
}
