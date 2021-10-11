package unity.map.objectives.types;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import unity.map.cinematic.*;
import unity.map.objectives.*;
import unity.map.objectives.ObjectiveModel.*;
import unity.util.*;

import static mindustry.Vars.*;

/**
 * An objective that will complete once a team has gathered resources as described in {@link #items}.
 * @author GlennFolker
 */
public class ResourceAmountObj extends Objective{
    protected @Ignore Table container;
    public final ItemStack[] items;

    public final Team team;
    public final Color from;
    public final Color to;

    public static void setup(){
        ObjectiveModel.setup(ResourceAmountObj.class, Pal.accent, () -> Icon.crafting, (node, f) -> {
            var items = f.get("items", ItemStack.with());
            var team = f.get("team", state.rules.defaultTeam);

            var exec = f.get("executor", "function(objective){}");
            var func = JSBridge.compileFunc(JSBridge.unityScope, f.name() + "-executor.js", exec, 1);

            Object[] args = {null};
            var obj = new ResourceAmountObj(items, team, node, f.name(), e -> {
                args[0] = e;
                func.call(JSBridge.context, JSBridge.unityScope, JSBridge.unityScope, args);
            });
            obj.ext(f);

            return obj;
        });
    }

    public ResourceAmountObj(ItemStack[] items, Team team, StoryNode node, String name, Cons<ResourceAmountObj> executor){
        this(items, team, Color.lightGray, Color.green, node, name, executor);
    }

    public ResourceAmountObj(ItemStack[] items, Team team, Color from, Color to, StoryNode node, String name, Cons<ResourceAmountObj> executor){
        super(node, name, 1, executor);
        this.items = items;
        this.team = team;
        this.from = from;
        this.to = to;
    }

    @Override
    public void ext(FieldTranslator f){
        super.ext(f);
        if(f.has("from")) from.set(f.<Color>get("from"));
        if(f.has("to")) from.set(f.<Color>get("to"));
    }

    @Override
    public void init(){
        super.init();

        if(headless) return;
        ui.hudGroup.fill(table -> {
            table.name = name;

            table.actions(
                Actions.scaleTo(0f, 1f),
                Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out)
            );

            table.center().left();

            var cell = table.table(Styles.black6, t -> {
                container = t;

                var pane = t.pane(Styles.defaultPane, cont -> {
                    cont.defaults().pad(4f);

                    for(int i = 0; i < items.length; i++){
                        if(i > 0) cont.row();

                        var item = items[i];
                        cont.table(Styles.black3, hold -> {
                            hold.defaults().pad(4f);

                            hold.left();
                            hold.image(() -> item.item.uiIcon)
                                .size(iconMed);

                            hold.right();
                            hold.labelWrap(() -> {
                                int amount = Math.min(state.teams.playerCores().sum(b -> b.items.get(item.item)), item.amount);
                                return
                                    "[#" + Tmp.c1.set(from).lerp(to, (float)amount / (float)item.amount).toString() + "]" + amount +
                                    " []/ [accent]" + item.amount + "[]";
                            })
                                .grow()
                                .get()
                                .setAlignment(Align.right);
                        })
                            .height(40f)
                            .growX()
                            .left();
                    }
                })
                    .update(p -> {
                        if(p.hasScroll()){
                            var result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                            if(result == null || !result.isDescendantOf(p)){
                                Core.scene.setScrollFocus(null);
                            }
                        }
                    })
                    .grow()
                    .pad(4f, 0f, 4f, 4f)
                    .get();

                pane.setScrollingDisabled(true, false);
                pane.setOverscroll(false, false);
            })
                .minSize(300f, 48f)
                .maxSize(300f, 156f);

            cell.visible(() -> ui.hudfrag.shown && node.sector.valid() && container == cell.get());
        });
    }

    @Override
    public void update(){
        super.update();

        var core = state.teams.cores(team).firstOpt();
        if(core == null || !core.isValid()) return;

        completed = true;
        for(var item : items){
            completed = core.items.get(item.item) >= item.amount;
            if(!completed) break;
        }
    }

    @Override
    public void doFinalize(){
        super.doFinalize();
        if(container != null){
            container.actions(
                Actions.moveBy(-container.getWidth(), 0f, 2f, Interp.pow3In),
                Actions.visible(false),
                new Action(){
                    @Override
                    public boolean act(float delta){
                        container.parent.removeChild(container);
                        container = null;

                        return true;
                    }
                }
            );
        }
    }
}