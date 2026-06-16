package mod.extend;

import arc.Core;
import arc.assets.loaders.TextureLoader.TextureParameter;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.Texture.TextureWrap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.gl.FrameBuffer;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.math.geom.Vec2;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mod.extend.starmap.HexCoord;
import mod.extend.starmap.HexGrid;
import mod.extend.starmap.HexPlanetMap;
import arc.func.Cons;
import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import mindustry.type.Sector;
import mod.extend.sector.PlanetLogistics;
import mod.extend.sector.PlanetLogisticsData;
import mod.ModUI;
import mod.extend.starmap.StarMapPlanetData;
import mod.extend.starmap.StarMapPlanets;

import static mindustry.Vars.*;
import static mindustry.graphics.g3d.PlanetRenderer.camLength;

public class StarMapDialog extends BaseDialog {
    public final HexGrid hexGrid = new HexGrid();
    public final HexPlanetMap hexPlanets = new HexPlanetMap();

    public @Nullable Planet currentPlanet;
    public Table sectorTop = new Table();

    public boolean selectingDestination = false;
    public @Nullable Cons<Sector> onSectorSelected;
    public @Nullable Sector selectFrom;

    public Rect bounds = new Rect();
    public View view;

    public float bgParallax = 0.25f;
    public float bgScrollScale = 2700f;

    public @Nullable Texture spaceTexture;

    static final int maxPlanetPreviewsMobile = 32;

    final OrderedMap<PlanetPreviewKey, FrameBuffer> planetPreviews = new OrderedMap<>();
    final PlanetParams planetPreview = new PlanetParams();
    long lastPreviewClearTime;

    public StarMapDialog() {
        super("", Styles.defaultDialog);

        //scaling/drag input
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                view.setScale(Mathf.clamp(view.scaleX - amountY / 10f * view.scaleX, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                view.requestScroll();
                return super.mouseMoved(event, x, y);
            }
        });

        touchable = Touchable.enabled;

        addCaptureListener(new ElementGestureListener(){
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(view.lastZoom < 0){
                    view.lastZoom = view.scaleX;
                }

                view.setScale(Mathf.clamp(distance / initialDistance * view.lastZoom, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                view.lastZoom = view.scaleX;
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                view.panX += deltaX / view.scaleX;
                view.panY += deltaY / view.scaleY;
                view.moved = true;
                view.clamp();
            }
        });

        margin(0f);

        setup();

        rebuildButtons();

        onResize(this::rebuildButtons);

        released(() -> view.moved = false);

        loadSpaceTexture();
    }

    void loadSpaceTexture(){
        if(Core.assets.isLoaded("sprites/space.png", Texture.class)){
            spaceTexture = Core.assets.get("sprites/space.png", Texture.class);
        }else{
            Core.assets.load("sprites/space.png", Texture.class, new TextureParameter(){{
                magFilter = TextureFilter.linear;
                minFilter = TextureFilter.mipMapLinearLinear;
                wrapU = wrapV = TextureWrap.mirroredRepeat;
                genMipMaps = true;
            }}).loaded = t -> spaceTexture = t;
        }
    }

    @Override
    public Dialog show() {
        rebuildButtons();
        if (!selectingDestination) {
            currentPlanet = null;
        }
        refreshStarmapLogistics();
        updateSelectedPlanet();

        return super.show();
    }

    void refreshStarmapLogistics() {
        PlanetLogistics.flushAllStats();
        for (Planet planet : content.planets()) {
            if (PlanetLogistics.hasBase(planet)) {
                PlanetLogistics.refreshImportRates(planet);
            }
        }
    }

    public void showSectorSelect(@Nullable Sector from, Cons<Sector> callback) {
        selectingDestination = true;
        selectFrom = from;
        onSectorSelected = callback;
        currentPlanet = from != null ? from.planet : null;
        show();
        if (from != null) {
            view.focusHex(hexPlanets.planetCoord(from.planet));
        }
    }

    public static boolean isStarmapPlanet(Planet planet) {
        return StarMapPlanets.isStarmapPlanet(planet);
    }

    public static boolean isSelectableDestination(Planet planet, @Nullable Sector from) {
        if (!isStarmapPlanet(planet)) return false;
        if (!PlanetLogistics.hasBase(planet)) return false;
        return from == null || from.planet != planet;
    }

    public static @Nullable Sector resolveSector(Planet planet) {
        for (Sector sector : planet.sectors) {
            if (sector.hasBase()) return sector;
        }
        if (planet.startSector >= 0 && planet.startSector < planet.sectors.size) {
            return planet.sectors.get(planet.startSector);
        }
        return planet.sectors.isEmpty() ? null : planet.sectors.first();
    }

    void confirmSectorSelect() {
        if (currentPlanet == null || !isStarmapPlanet(currentPlanet) || onSectorSelected == null) return;

        Sector dest = resolveSector(currentPlanet);
        if (dest == null || dest == selectFrom) return;

        onSectorSelected.get(dest);
        hide();
    }

    @Override
    public void hide() {
        selectingDestination = false;
        onSectorSelected = null;
        selectFrom = null;
        super.hide();
    }

    public void setup() {
        clearChildren();
        stack(
                view = new View(),
                buttons
        ).grow();

        hexPlanets.rebuild();
    }

    public void rebuildButtons() {
        buttons.clearChildren();
        buttons.bottom();

        if (Core.graphics.isPortrait()) {
            buttons.add(sectorTop).colspan(2).fillX();
            buttons.row();
            buttons.table(t -> {
                t.button("@back", Icon.left, this::hide).size(200f, 54f).pad(2).bottom();
                t.button("Star Map", Icon.tree, () -> {}).size(200f, 54f).pad(2).bottom();
            }).pad(0).margin(0);
            buttons.row();
        } else {
            buttons.button("@back", Icon.left, this::hide).size(200f, 54f).pad(2).bottom();
            buttons.add().growX();
            buttons.add(sectorTop).minWidth(280f);
            buttons.add().growX();
            buttons.table(t -> {
                t.button("Star Map", Icon.planet, () -> {}).size(200f, 54f).pad(2).bottom();
            }).bottom().pad(0).margin(0);
        }
    }

    void updateSelectedPlanet() {
        Planet planet = currentPlanet;
        Table stable = sectorTop;

        if (planet == null) {
            stable.clear();
            stable.visible = selectingDestination;
            if (selectingDestination) {
                stable.background(Styles.black6);
                stable.add("Select Destination").wrap().width(260f).pad(4f);
            }
            return;
        }

        stable.visible = true;
        stable.clear();
        stable.background(Styles.black6);

        Sector sector = isStarmapPlanet(planet) ? resolveSector(planet) : null;

        stable.table(title -> {
            title.add("[accent]" + planet.localizedName).padLeft(3).row();
            if (sector != null) {
                title.add(sector.name()).color(Pal.gray).padLeft(3).row();
            }
        }).pad(0).row();
        stable.image().color(Pal.accent).fillX().height(3f).pad(3f).row();

        if (sector != null && sector.hasBase()) {
            stable.button("@stats", Icon.info, Styles.cleart, () -> ModUI.planetLogisticsStats.show(planet, sector))
                    .height(40f).fillX().row();
        }

        if (selectingDestination) {
            stable.button("@sectors.launch", Icon.upOpen, this::confirmSectorSelect)
                    .disabled(currentPlanet == null || !isSelectableDestination(currentPlanet, selectFrom))
                    .size(200f, 54f).bottom().row();
        } else {
            stable.button("View Planet", Icon.eye, () -> {
                ui.planet.viewPlanet(planet, false);
                this.hide();
            }).size(200f, 54f).bottom();
        }
    }

    static final Color payloadLogisticsColor = Pal.ammo;
    static final Color itemLogisticsColor = Pal.items;
    static final Color liquidLogisticsColor = Pal.techBlue;

    enum LogisticsLane {
        payload(-1f, payloadLogisticsColor),
        item(0f, itemLogisticsColor),
        liquid(1f, liquidLogisticsColor);

        final float index;
        final Color color;

        LogisticsLane(float index, Color color) {
            this.index = index;
            this.color = color;
        }
    }

    static class LogisticsLink {
        final StarMapPlanetData from, to;
        final LogisticsLane lane;
        final boolean exportFromSelected;

        LogisticsLink(StarMapPlanetData from, StarMapPlanetData to, LogisticsLane lane, boolean exportFromSelected) {
            this.from = from;
            this.to = to;
            this.lane = lane;
            this.exportFromSelected = exportFromSelected;
        }
    }

    void eachLogisticsLink(@Nullable Planet selectedPlanet, Cons<LogisticsLink> cons) {
        ObjectSet<String> seen = new ObjectSet<>();

        if (selectedPlanet != null && PlanetLogistics.hasBase(selectedPlanet)) {
            PlanetLogisticsData selectedData = PlanetLogistics.get(selectedPlanet);

            for (StarMapPlanetData otherEntry : StarMapPlanets.all) {
                Planet other = otherEntry.planet;
                if (!isStarmapPlanet(other) || other == selectedPlanet) continue;
                if (!PlanetLogistics.hasBase(other)) continue;

                PlanetLogisticsData otherData = PlanetLogistics.get(other);
                String importKey = other.name + "->" + selectedPlanet.name;

                if (otherData.destinationPlanet() == selectedPlanet) {
                    addLink(cons, seen, importKey, otherEntry, StarMapPlanets.get(selectedPlanet), otherData, false);
                }

                if (selectedData.destinationPlanet() == other) {
                    addLink(cons, seen, selectedPlanet.name + "->" + other.name, StarMapPlanets.get(selectedPlanet), otherEntry, selectedData, true);
                }
            }
            return;
        }

        for (StarMapPlanetData fromEntry : StarMapPlanets.all) {
            Planet from = fromEntry.planet;
            if (!isStarmapPlanet(from)) continue;
            if (!PlanetLogistics.hasBase(from)) continue;

            Planet dest = PlanetLogistics.get(from).destinationPlanet();
            StarMapPlanetData toEntry = dest == null ? null : StarMapPlanets.get(dest);
            if (toEntry == null || !isStarmapPlanet(dest) || from == dest) continue;

            String key = from.name + "->" + dest.name;
            if (seen.contains(key)) continue;
            seen.add(key);

            addLink(cons, seen, key, fromEntry, toEntry, PlanetLogistics.get(from), true);
        }
    }

    void addLink(Cons<LogisticsLink> cons, ObjectSet<String> seen, String key, StarMapPlanetData from, StarMapPlanetData to, PlanetLogisticsData data, boolean exportFromSelected) {
        if (data.anyPayloadExports()) tryAddLink(cons, seen, key + ":payload", from, to, LogisticsLane.payload, exportFromSelected);
        if (data.anyItemExports()) tryAddLink(cons, seen, key + ":item", from, to, LogisticsLane.item, exportFromSelected);
        if (data.anyLiquidExports()) tryAddLink(cons, seen, key + ":liquid", from, to, LogisticsLane.liquid, exportFromSelected);
    }

    void tryAddLink(Cons<LogisticsLink> cons, ObjectSet<String> seen, String key, StarMapPlanetData from, StarMapPlanetData to, LogisticsLane lane, boolean exportFromSelected) {
        if (seen.contains(key)) return;
        seen.add(key);
        cons.get(new LogisticsLink(from, to, lane, exportFromSelected));
    }

    void drawLogisticsArc(float x1, float y1, float x2, float y2, Color from, Color to, float laneIndex, float alpha, float stroke) {
        float ang = Mathf.angle(x2 - x1, y2 - y1);
        float len = Mathf.dst(x1, y1, x2, y2);
        float pad = hexGrid.size * 0.55f;
        if (len <= pad * 1.2f) return;

        float perpX = Mathf.cosDeg(ang + 90f);
        float perpY = Mathf.sinDeg(ang + 90f);
        float laneSpacing = hexGrid.lineWidth * 2.8f;
        float laneOffset = laneIndex * laneSpacing;

        float sx = x1 + Mathf.cosDeg(ang) * pad + perpX * laneOffset;
        float sy = y1 + Mathf.sinDeg(ang) * pad + perpY * laneOffset;
        float ex = x2 - Mathf.cosDeg(ang) * pad + perpX * laneOffset;
        float ey = y2 - Mathf.sinDeg(ang) * pad + perpY * laneOffset;

        float fin = (Time.globalTime / 90f) % 1f, fout = 1 - fin;
        Tmp.c1.set(from).lerp(to, fout);
        Tmp.v1.set(sx, sy).lerp(ex, ey, fin);
        Draw.alpha(alpha);
        Lines.stroke(stroke);
        Lines.line(sx, sy, Tmp.c1, Tmp.v1.x, Tmp.v1.y, to);
        Lines.line(Tmp.v1.x, Tmp.v1.y, from, ex, ey, Tmp.c1);

        Draw.reset();
    }

    @Nullable FrameBuffer getPlanetPreviewBuffer(Planet planet, int size) {
        if (planet.mesh == null || size < 1) return null;
        if (!selectingDestination && !planet.visible) return null;

        if (mobile && Time.timeSinceMillis(lastPreviewClearTime) > 2000 && planetPreviews.size > maxPlanetPreviewsMobile) {
            Seq<PlanetPreviewKey> keys = planetPreviews.orderedKeys().copy();
            for (int i = 0; i < planetPreviews.size - maxPlanetPreviewsMobile; i++) {
                planetPreviews.remove(keys.get(i)).dispose();
            }
            lastPreviewClearTime = Time.millis();
        }

        PlanetPreviewKey key = new PlanetPreviewKey(planet, size);
        if (!planetPreviews.containsKey(key)) {
            planetPreviews.put(key, createPlanetPreviewBuffer(planet, size));
        }

        return planetPreviews.get(key);
    }

    FrameBuffer createPlanetPreviewBuffer(Planet planet, int size) {
        Draw.flush();

        FrameBuffer buffer = new FrameBuffer(size, size);
        buffer.begin(Color.clear);

        setupPlanetPreview(planet, size);
        renderer.planets.render(planetPreview);

        buffer.end();
        Draw.flush();
        return buffer;
    }

    void setupPlanetPreview(Planet planet, int size) {
        planetPreview.planet = planet;
        planetPreview.viewW = size;
        planetPreview.viewH = size;
        planetPreview.drawUi = false;
        planetPreview.drawSkybox = false;
        planetPreview.alwaysDrawAtmosphere = true;
        planetPreview.uiAlpha = 0f;
        planetPreview.zoom = 1f;
        planetPreview.renderer = null;
        planetPreview.otherCamPos = null;
        planetPreview.otherCamAlpha = 0f;
        planetPreview.camPos.set(0f, camLength, 0.1f);
    }

    static final class PlanetPreviewKey {
        final Planet planet;
        final int size;

        PlanetPreviewKey(Planet planet, int size) {
            this.planet = planet;
            this.size = size;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PlanetPreviewKey other && other.planet == planet && other.size == size;
        }

        @Override
        public int hashCode() {
            return planet.hashCode() * 31 + size;
        }
    }

    void drawPlanetFallback(float px, float py, float size, Planet planet, float alpha){
        float radius = size / 2f;

        Draw.z(1f);
        Draw.color(planet.iconColor, alpha * 0.85f);
        Fill.circle(px, py, radius);

        Lines.stroke(Scl.scl(2f), planet.iconColor);
        Draw.alpha(alpha);
        Lines.circle(px, py, radius);

        float isize = radius * 1.6f;
        Draw.color(Color.white, alpha);
        Draw.rect(Icon.planet.getRegion(), px, py, isize, isize);
    }

    void drawSpaceBackground(float viewWidth, float viewHeight, float panX, float panY, float scaleX, float scaleY, float alpha){
        if(spaceTexture == null) return;
        if(scaleX <= 0f) scaleX = 1f;
        if(scaleY <= 0f) scaleY = 1f;

        float scrollX = -panX * bgParallax / (bgScrollScale * scaleX) - 0.1f;
        float scrollY = panY * bgParallax / (bgScrollScale * scaleY) - 0.1f;

        Draw.z(-1f);
        Draw.color(1f, 1f, 1f, alpha);
        Tmp.tr1.set(Draw.wrap(spaceTexture));
        Tmp.tr1.scroll(scrollX, scrollY);
        Draw.rect(Tmp.tr1, viewWidth / 2f, viewHeight / 2f, viewWidth / scaleX, viewHeight / scaleY);
        Draw.color();
    }

    public class View extends Group {
        public float panX = 0, panY = -30, lastZoom = -1;
        public boolean moved = false;
        public @Nullable HexCoord hoverHex;

        {
            rebuildAll();
        }

        public void rebuildAll(){
            setOrigin(Align.center);
            setTransform(true);

            clearListeners();

            //add listener to the background rect, so it doesn't get unnecessary touch input
            addListener(new ElementGestureListener(){
                @Override
                public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                    if(moved || button != KeyCode.mouseLeft) return;

                    updateHoverHex(x, y);
                    Planet planet = hexPlanets.planetAt(hoverHex);

                    if (planet != null && StarMapPlanets.has(planet)) {
                        currentPlanet = planet;
                        updateSelectedPlanet();
                    } else if (!selectingDestination && planet != null) {
                        currentPlanet = planet;
                        updateSelectedPlanet();
                    }
                }

                @Override
                public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    super.touchDown(event, x, y, pointer, button);
                }
            });

            addListener(new InputListener(){
                @Override
                public boolean mouseMoved(InputEvent event, float x, float y){
                    updateHoverHex(x, y);
                    return false;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                    hoverHex = null;
                }
            });
        }

        void focusHex(HexCoord hex) {
            panX = -hexGrid.axis0WorldStep() * hex.a;
            panY = -hexGrid.axis120WorldStep() * (hex.c + hex.a * 0.5f);
        }

        @Nullable HexCoord currentSectorHex() {
            Sector sector = selectingDestination ? selectFrom : (state.isCampaign() ? state.rules.sector : null);
            if (sector == null) return null;
            return hexPlanets.planetCoord(sector.planet);
        }

        void updateHoverHex(float localX, float localY){
            hoverHex = hexGrid.localToHex(localX, localY, panX, panY, width, height);
        }

        void clamp(){
        }

        @Override
        public void act(float delta){
            super.act(delta);
            if(hasMouse()){
                Tmp.v1.set(Core.input.mouseX(), Core.input.mouseY());
                screenToLocalCoordinates(Tmp.v1);
                updateHoverHex(Tmp.v1.x, Tmp.v1.y);
            }
        }

        @Override
        public void draw(){
            clamp();

            float clipW = width;
            float clipH = height;
            float clipX = (width - clipW) / 2f;
            float clipY = (height - clipH) / 2f;
            if(clipBegin(clipX, clipY, clipW, clipH)){
                if(transform) applyTransform(computeTransform());

                drawSpaceBackground(width, height, panX, panY, scaleX, scaleY, parentAlpha);

                float offsetX = panX + width / 2f, offsetY = panY + height / 2f;
                drawPlanetHexGlow(offsetX, offsetY);
                hexGrid.draw(width, height, scaleX, scaleY, offsetX, offsetY, hoverHex, parentAlpha);
                drawCurrentSectorHighlight(offsetX, offsetY);
                drawSelectedHexHighlight(offsetX, offsetY);
                drawPlanets(offsetX, offsetY);
                drawLogisticsArrows(offsetX, offsetY);
                super.drawChildren();

                if(transform) resetTransform();
                clipEnd();
            }
        }

        void drawLogisticsArrows(float offsetX, float offsetY) {
            Draw.z(0.65f);
            Planet selectedPlanet = currentPlanet;

            eachLogisticsLink(selectedPlanet, link -> {
                hexGrid.hexToWorld(hexPlanets.planetCoord(link.from), offsetX, offsetY, Tmp.v1);
                hexGrid.hexToWorld(hexPlanets.planetCoord(link.to), offsetX, offsetY, Tmp.v2);

                boolean highlight = selectedPlanet != null && (selectedPlanet == link.from.planet || selectedPlanet == link.to.planet);
                float alpha = parentAlpha * (highlight ? 0.8f : 0.4f);
                float stroke = hexGrid.lineWidth * (highlight ? 1.5f : 1f);

                if (!highlight && selectedPlanet == null) {
                    Tmp.c2.set(link.lane.color).lerp(Pal.gray, 0.5f);
                    Tmp.c3.set(Pal.gray).a(0.5f);
                } else if (!link.exportFromSelected) {
                    Tmp.c2.set(link.lane.color);
                    Tmp.c3.set(Pal.gray);
                } else {
                    Tmp.c2.set(link.lane.color);
                    Tmp.c3.set(Pal.gray);
                }

                drawLogisticsArc(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, Tmp.c2, Tmp.c3, link.lane.index, alpha, stroke);
            });

            Draw.reset();
        }

        void drawPlanets(float offsetX, float offsetY){
            drawPlanetOrbits(offsetX, offsetY);
            for(StarMapPlanetData data : StarMapPlanets.all){
                drawPlanet(data, offsetX, offsetY);
            }
        }

        void drawPlanetOrbits(float offsetX, float offsetY){
            Draw.z(0.5f);

            for(StarMapPlanetData childData : StarMapPlanets.all){
                Planet child = childData.planet;
                StarMapPlanetData parentData = StarMapPlanets.get(child.parent);
                if(parentData == null || !child.drawOrbit) continue;

                hexGrid.hexToWorld(hexPlanets.planetCoord(childData), offsetX, offsetY, Tmp.v1);
                hexGrid.hexToWorld(hexPlanets.planetCoord(parentData), offsetX, offsetY, Tmp.v2);

                float orbitRadius = Tmp.v1.dst(Tmp.v2);
                if(orbitRadius < hexGrid.size * 0.25f) continue;
                Lines.stroke(hexGrid.lineWidth);

                Draw.color(Pal.gray);
                Draw.alpha(parentAlpha * 0.4f);
                Lines.circle(Tmp.v2.x, Tmp.v2.y, orbitRadius);

                Draw.color(child.iconColor);
                Draw.alpha(parentAlpha * 0.25f);
                Lines.line(Tmp.v2.x, Tmp.v2.y, Tmp.v1.x, Tmp.v1.y);
            }

            for(StarMapPlanetData fromData : StarMapPlanets.all){
                hexGrid.hexToWorld(hexPlanets.planetCoord(fromData), offsetX, offsetY, Tmp.v1);

                for(StarMapPlanetData linkData : fromData.links){
                    hexGrid.hexToWorld(hexPlanets.planetCoord(linkData), offsetX, offsetY, Tmp.v2);

                    Lines.stroke(hexGrid.lineWidth * 1.25f);
                    Draw.color(Pal.accent);
                    Draw.alpha(parentAlpha * 0.4f);
                    Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                }
            }

            Draw.reset();
        }

        void drawPlanetHexGlow(float offsetX, float offsetY){
            HexCoord selectedHex = selectedHex();

            Draw.z(0.05f);
            for(StarMapPlanetData data : StarMapPlanets.all){
                HexCoord hex = hexPlanets.planetCoord(data);
                if(selectedHex != null && hex.equals(selectedHex)) continue;
                if(currentSectorHex() != null && hex.equals(currentSectorHex())) continue;

                hexGrid.hexToWorld(hex, offsetX, offsetY, Tmp.v1);
                Draw.color(Color.white, parentAlpha * 0.08f);
                Fill.poly(Tmp.v1.x, Tmp.v1.y, 6, hexGrid.size, hexGrid.rotation);
            }
            Draw.reset();
        }

        void drawCurrentSectorHighlight(float offsetX, float offsetY) {
            HexCoord hex = currentSectorHex();
            if (hex == null) return;

            HexCoord selected = selectedHex();
            if (selected != null && hex.equals(selected)) return;

            hexGrid.hexToWorld(hex, offsetX, offsetY, Tmp.v1);

            Draw.z(0.12f);
            Draw.color(Pal.heal, parentAlpha * 0.35f);
            Fill.poly(Tmp.v1.x, Tmp.v1.y, 6, hexGrid.size, hexGrid.rotation);
            Lines.stroke(hexGrid.lineWidth * 2f, Pal.heal);
            Draw.alpha(parentAlpha * 0.85f);
            Lines.poly(Tmp.v1.x, Tmp.v1.y, 6, hexGrid.size, hexGrid.rotation);
            Draw.reset();
        }

        void drawSelectedHexHighlight(float offsetX, float offsetY){
            HexCoord selectedHex = selectedHex();
            if(selectedHex == null) return;

            hexGrid.hexToWorld(selectedHex, offsetX, offsetY, Tmp.v1);

            Draw.z(0.15f);
            Draw.color(Pal.accent, parentAlpha * 0.4f);
            Fill.poly(Tmp.v1.x, Tmp.v1.y, 6, hexGrid.size, hexGrid.rotation);
            Lines.stroke(hexGrid.lineWidth * 2f, Pal.accent);
            Draw.alpha(parentAlpha);
            Lines.poly(Tmp.v1.x, Tmp.v1.y, 6, hexGrid.size, hexGrid.rotation);
            Draw.reset();
        }

        @Nullable HexCoord selectedHex(){
            return currentPlanet == null ? null : hexPlanets.planetCoord(currentPlanet);
        }

        void drawPlanet(StarMapPlanetData data, float offsetX, float offsetY){
            Planet planet = data.planet;
            hexGrid.hexToWorld(hexPlanets.planetCoord(data), offsetX, offsetY, Tmp.v1);
            float px = Tmp.v1.x, py = Tmp.v1.y;
            float size = Scl.scl(planet.radius) * data.starmapSize;
            int texSize = (int) Math.max(hexGrid.size, Mathf.ceil(size) * hexGrid.size);
            float drawAlpha = parentAlpha * (selectingDestination && planet == selectFromPlanet() ? 0.55f : 1f);

            Draw.z(1f);
            FrameBuffer preview = getPlanetPreviewBuffer(planet, texSize);
            if (preview != null) {
                Draw.color(Color.white, drawAlpha);
                Draw.rect(Draw.wrap(preview.getTexture()), px, py, size * hexGrid.size, -size * hexGrid.size);
            } else {
                drawPlanetFallback(px, py, size * hexGrid.size / 2f, planet, drawAlpha);
            }

            Draw.reset();
        }

        @Nullable Planet selectFromPlanet() {
            return selectFrom == null ? null : selectFrom.planet;
        }
    }
}
