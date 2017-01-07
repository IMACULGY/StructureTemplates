/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.structureTemplates.internal.systems;

import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.clipboard.ClipboardManager;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.rendering.logic.RegionOutlineComponent;
import org.terasology.structureTemplates.internal.components.EditsCopyRegionComponent;
import org.terasology.structureTemplates.internal.events.CopyBlockRegionResultEvent;

/**
 * Handles the result of the activation of the copyBlockRegionTool item.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class StructureTemplateEditorClientSystem extends BaseComponentSystem {

    @In
    private ClipboardManager clipboardManager;

    @In
    private LocalPlayer locatPlayer;

    @In
    private EntityManager entityManager;

    @In
    private InventoryManager inventoryManager;

    private EntityRef regionOutlineEntity = EntityRef.NULL;
    private EntityRef highlightedEditorEntity = EntityRef.NULL;


    @ReceiveEvent
    public void onCopyBlockRegionResultEvent(CopyBlockRegionResultEvent event, EntityRef entity) {
        clipboardManager.setClipboardContents(event.getJson());
    }

    @ReceiveEvent
    public void onAddedCopyBlockRegionComponent(OnAddedComponent event, EntityRef entity,
                                                StructureTemplateEditorComponent component) {
        updateHighlightedEditorEntity();
    }

    @ReceiveEvent
    public void onChangedCopyBlockRegionComponent(OnChangedComponent event, EntityRef entity,
                                             StructureTemplateEditorComponent component) {
        updateHighlightedEditorEntity();
    }

    @ReceiveEvent
    public void onBeforeRemoveCopyBlockRegionComponent(BeforeRemoveComponent event, EntityRef entity,
                                                  StructureTemplateEditorComponent component) {
        if (entity.equals(highlightedEditorEntity)) {
            setHighlightedEditorEntity(EntityRef.NULL);
        }
        // Calling updateOutlineEntity is not possible as component is still there
    }


    @ReceiveEvent
    public void onAddedEditsCopyRegionComponent(OnAddedComponent event, EntityRef entity,
                                                EditsCopyRegionComponent component) {
        updateHighlightedEditorEntity();
    }

    @ReceiveEvent
    public void onChangedEditsCopyRegionComponent(OnChangedComponent event, EntityRef entity,
                                                  EditsCopyRegionComponent component) {
        updateHighlightedEditorEntity();
    }

    @ReceiveEvent
    public void onBeforeRemoveEditsCopyRegionComponent(BeforeRemoveComponent event, EntityRef entity,
                                                       EditsCopyRegionComponent component) {
        if (entity.equals(locatPlayer.getClientEntity())) {
            setHighlightedEditorEntity(EntityRef.NULL);
        }
    }

    public void destoryOutlineEntiy() {
        if (regionOutlineEntity.exists()) {
            regionOutlineEntity.destroy();
        }
        highlightedEditorEntity = EntityRef.NULL;
    }

    public void updateOutlineEntity() {
        Region3i region3i = getRegionToDraw();
        if (region3i == null) {
            destoryOutlineEntiy();
        } else {
            if (regionOutlineEntity.exists()) {
                RegionOutlineComponent oldComponent = regionOutlineEntity.getComponent(RegionOutlineComponent.class);
                if (oldComponent != null && oldComponent.corner1 != null && oldComponent.corner2 != null) {
                    Region3i oldRegion = Region3i.createBounded(oldComponent.corner1, oldComponent.corner2);
                    if (oldRegion.equals(region3i)) {
                        return;
                    }
                }
            }
            if (regionOutlineEntity.exists()) {
                RegionOutlineComponent regionOutlineComponent = regionOutlineEntity.getComponent(RegionOutlineComponent.class);
                regionOutlineComponent.corner1 = new Vector3i(region3i.min());
                regionOutlineComponent.corner2 = new Vector3i(region3i.max());
                regionOutlineEntity.saveComponent(regionOutlineComponent);
            } else {
                EntityBuilder entityBuilder = entityManager.newBuilder();
                entityBuilder.setPersistent(false);
                RegionOutlineComponent regionOutlineComponent = new RegionOutlineComponent();
                regionOutlineComponent.corner1 = new Vector3i(region3i.min());
                regionOutlineComponent.corner2 = new Vector3i(region3i.max());
                entityBuilder.addComponent(regionOutlineComponent);
                regionOutlineEntity = entityBuilder.build();
            }
        }
    }


    private void setHighlightedEditorEntity(EntityRef entityRef) {
        highlightedEditorEntity = entityRef;
        updateOutlineEntity();
    }

    private void updateHighlightedEditorEntity() {
        EntityRef clientEntity = locatPlayer.getClientEntity();

        EditsCopyRegionComponent editsCopyRegionComponent = clientEntity.getComponent(EditsCopyRegionComponent.class);
        if (editsCopyRegionComponent == null) {
            setHighlightedEditorEntity(EntityRef.NULL);
        } else {
            setHighlightedEditorEntity(editsCopyRegionComponent.structureTemplateEditor);
        }
    }


    private Region3i getRegionToDraw() {

        StructureTemplateEditorComponent structureTemplateEditorComponent = highlightedEditorEntity.getComponent(StructureTemplateEditorComponent.class);
        if (structureTemplateEditorComponent == null) {
            return null;
        }

        if (structureTemplateEditorComponent.origin == null) {
            return null;
        }

        Region3i region = structureTemplateEditorComponent.editRegion;
        region = region.move(structureTemplateEditorComponent.origin);
        return region;
    }
}
