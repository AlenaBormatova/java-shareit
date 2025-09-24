package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByOwnerIdOrderById(Long ownerId);

    @Query("SELECT i FROM Item i " +
            "WHERE (UPPER(i.name) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR UPPER(i.description) LIKE UPPER(CONCAT('%', :text, '%'))) " +
            "AND i.available = true")
    List<Item> searchAvailableByNameOrDescription(@Param("text") String text);

    boolean existsByNameAndOwnerId(String name, Long ownerId);

    List<Item> findByRequestId(Long requestId);

    @Query("SELECT i FROM Item i WHERE i.owner.id = :ownerId AND i.name = :name")
    Optional<Item> findByOwnerIdAndName(@Param("ownerId") Long ownerId,
                                        @Param("name") String name);
}