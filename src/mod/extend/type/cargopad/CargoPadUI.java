package mod.extend.type.cargopad;

import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class CargoPadUI {
    public static void buildPayloadTable(Block block, Table table, Seq<UnlockableContent> options, arc.func.Prov<UnlockableContent> current, arc.func.Cons<UnlockableContent> callback, int columns) {
        table.pane(inner -> {
            int index = 0;
            for (UnlockableContent content : options) {
                if (!content.unlockedNow()) continue;
                inner.button(new TextureRegionDrawable(content.uiIcon), Styles.clearTogglei, () -> callback.get(content))
                        .size(40f)
                        .pad(4f)
                        .checked(b -> current.get() == content);
                if (++index % columns == 0) inner.row();
            }
        }).grow();
    }

    public static void buildDestinationButton(Table table, Runnable onSelect) {
        table.row();
        table.button(Icon.upOpen, Styles.cleari, onSelect).size(40f);
    }

    public static Seq<UnlockableContent> payloadOptions() {
        Seq<UnlockableContent> options = new Seq<>();
        content.blocks().each(options::add);
        content.units().each(options::add);
        return options;
    }
}
