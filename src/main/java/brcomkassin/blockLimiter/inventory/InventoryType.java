package brcomkassin.blockLimiter.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InventoryType {

    LIMITER("§6Limites de Itens",54);

    private final String name;
    private final int size;

}
