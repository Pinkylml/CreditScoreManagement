package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.domain.model.*;
import com.montran.creditscore.domain.port.outbound.UserStore;
import com.montran.creditscore.domain.exception.PersistenceException;
import com.montran.creditscore.infrastructure.persistence.dto.CreditHistoryStorageDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import java.math.BigDecimal;

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

/**
 * Base class for file-backed user stores. Implements the Template Method pattern:
 * this class owns caching, locking, and DTO mapping logic while subclasses
 * handle the actual file format (XML, JSON, etc.) via {@link #readFromFile()} and
 * {@link #writeToFile(List)}.
 *
 * <p>Thread safety is provided by a {@link StampedLock}. Reads first attempt an
 * optimistic read; if a concurrent write is detected, they fall back to a full read lock.
 * Writes always use an exclusive write lock and immediately flush the cache to disk.</p>
 */
public abstract class AbstractFileUserStore implements UserStore {

    // In-memory cache of all users, keyed by SSN.
    protected final ConcurrentMap<String, User> cache = new ConcurrentHashMap<>();

    protected final StampedLock lock = new StampedLock();

    // SimpleDateFormat is not thread-safe; a new instance is created per call.
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /** Loads existing data from disk into the cache on startup. */
    protected AbstractFileUserStore() {
        loadStateIntoCache();
    }

    /**
     * Reads all user records from the underlying file.
     * Called once at startup and whenever the cache needs to be rebuilt.
     */
    protected abstract List<UserStorageDto> readFromFile();

    /**
     * Writes the complete current state to the underlying file.
     * Called on every save or delete to keep the file in sync.
     */
    protected abstract void writeToFile(List<UserStorageDto> dtos);

    @Override
    public Optional<User> findBySsn(String ssn) {
        long stamp = lock.tryOptimisticRead();
        User user = cache.get(ssn);
        // If a write occurred during our optimistic read, fall back to a strict read lock.
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                user = cache.get(ssn);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        // Return a defensive copy so callers cannot mutate the cached aggregate directly.
        return (user != null) ? Optional.of(new User(user)) : Optional.empty();
    }

    @Override
    public List<User> findAll() {
        long stamp = lock.readLock();
        try {
            // Return defensive copies so callers cannot mutate the cached aggregates.
            return cache.values().stream()
                    .map(User::new)
                    .collect(Collectors.toList());
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

    /** Serializes the entire cache to DTOs and delegates writing to the subclass. */
    private void flushCacheToFile() {
        List<UserStorageDto> dtos = cache.values().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        writeToFile(dtos);
    }

    /** Reads DTOs from disk and populates the cache. Runs under a write lock to prevent partial reads. */
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

    /** Converts a domain User into a flat DTO suitable for serialization. */
    private UserStorageDto mapToDto(User user) {
        UserStorageDto dto = new UserStorageDto();
        dto.setSsn(user.getSsn());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        dto.setEmail(user.getEmail());
        dto.setCreditScore(user.getCreditScore());
        dto.setRiskLevel(user.getRiskLevel().name());
        dto.setTotalCreditLimit(user.getTotalCreditLimit().toPlainString());

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        List<CreditHistoryStorageDto> historyDtos = new ArrayList<>();

        for (CreditHistoryRecord record : user.getCreditHistory()) {
            CreditHistoryStorageDto recDto = new CreditHistoryStorageDto();

            recDto.setDueDateStr(sdf.format(record.getDueDate()));
            if (record.getSettlementDate() != null) {
                recDto.setSettlementDateStr(sdf.format(record.getSettlementDate()));
            }

            recDto.setTransactionType(record.getTransactionType().name());
            recDto.setAmount(record.getAmount().toPlainString());
            recDto.setStatus(record.getStatus().name());
            recDto.setTransactionId(record.getTransactionId());
            recDto.setDueDateStr(sdf.format(record.getDueDate()));

            historyDtos.add(recDto);
        }

        dto.setCreditHistory(historyDtos);
        return dto;
    }

    /** Rebuilds a domain User from a deserialized DTO. Throws if a date string is malformed. */
    private User mapToDomain(UserStorageDto dto) {
        User user = new User(dto.getSsn(), dto.getName(), dto.getAddress(), dto.getEmail(),
                new BigDecimal(dto.getTotalCreditLimit()));
        user.setCreditScore(dto.getCreditScore());
        user.setRiskLevel(RiskLevel.valueOf(dto.getRiskLevel()));

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (dto.getCreditHistory() != null) {
            for (CreditHistoryStorageDto recDto : dto.getCreditHistory()) {
                try {
                    Date dueDate = sdf.parse(recDto.getDueDateStr());
                    Date settlementDate = null;

                    if (recDto.getSettlementDateStr() != null && !recDto.getSettlementDateStr().isEmpty()) {
                        settlementDate = sdf.parse(recDto.getSettlementDateStr());
                    }

                    CreditHistoryRecord record = new CreditHistoryRecord(
                            recDto.getTransactionId(),
                            dueDate,
                            settlementDate,
                            TransactionType.valueOf(recDto.getTransactionType()),
                            new BigDecimal(recDto.getAmount()),
                            TransactionStatus.valueOf(recDto.getStatus()));

                    user.addCreditRecord(record);
                } catch (ParseException e) {
                    throw new PersistenceException(
                            "Corrupted date format in persistence file for transaction '"
                                    + recDto.getTransactionId() + "'.", e);
                }
            }
        }
        return user;
    }
}
