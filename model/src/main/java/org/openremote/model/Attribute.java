/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.asset.AssetMeta;

import java.util.NoSuchElementException;

import static org.openremote.model.asset.AssetMeta.PROTECTED;
import static org.openremote.model.asset.AssetMeta.READ_ONLY;
import static org.openremote.model.asset.AssetMeta.STORE_DATA_POINTS;

/**
 * Convenience overlay API for {@link JsonObject}.
 * <p>
 * Modifies the given or an empty object.
 */
public class Attribute extends AbstractValueTimestampHolder<Attribute> {

    /**
     * Attribute names should be very simple, as we use them in SQL path expressions, etc. and must manually escape.
     */
    public static final String ATTRIBUTE_NAME_PATTERN = "^\\w+$";

    protected String name;

    public Attribute() {
        super(Json.createObject());
    }

    public Attribute(String name) {
        this(name, Json.createObject());
    }

    public Attribute(String name, AttributeType type) {
        this(name);
        setType(type);
    }

    public Attribute(String name, JsonObject jsonObject) {
        super(jsonObject);
        this.name = name;
    }

    public Attribute(String name, AttributeType type, JsonValue value) {
        this(name, Json.createObject());
        setType(type);
        setValue(value);
    }

    @Override
    protected boolean isValidValue(JsonValue value) {
        return getType() != null
            ? getType().isValid(value)
            : super.isValidValue(value);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public AttributeType getType() {
        String typeName = jsonObject.hasKey("type") ? jsonObject.get("type").asString() : null;
        return typeName != null ? AttributeType.fromValue(typeName) : null;
    }

    public Attribute setType(AttributeType type) {
        if (type != null) {
            jsonObject.put("type", Json.create(type.getValue()));
        } else if (jsonObject.hasKey("type")) {
            jsonObject.remove("type");
        }
        return this;
    }

    public boolean hasMeta() {
        return jsonObject.hasKey("meta");
    }

    public Meta getMeta() {
        return hasMeta() ? new Meta(jsonObject.getArray("meta")) : null;
    }

    public Attribute setMeta(Meta meta) {
        if (meta != null) {
            jsonObject.put("meta", meta.getJsonArray());
        } else if (jsonObject.hasKey("meta")) {
            jsonObject.remove("meta");
        }
        return this;
    }

    public boolean hasMetaItem(String name) {
        return hasMeta() && getMeta().contains(name);
    }

    public MetaItem firstMetaItem(String name) {
        return hasMetaItem(name) ? getMeta().first(name) : null;
    }

    public MetaItem firstMetaItemOrThrow(String name) throws NoSuchElementException {
        if (!hasMetaItem(name))
            throw new NoSuchElementException("Missing item: " + name);
        return firstMetaItem(name);
    }

    public Attribute copy() {
        return new Attribute(getName(), Json.parse(getJsonObject().toJson()));
    }

    public boolean isValid() {
        return getName() != null && getName().length() > 0 && getType() != null;
    }

    public AttributeRef getAttributeRef(String entityId) {
        return new AttributeRef(entityId, getName());
    }

    /**
     * @return The current value.
     */
    public AttributeState getState(String assetId) {
        return new AttributeState(
            getAttributeRef(assetId),
            getValue()
        );
    }

    /**
     * @return The current value and its timestamp represented as an attribute event.
     */
    public AttributeEvent getStateEvent(String assetId) {
        return new AttributeEvent(
            getAttributeRef(assetId),
            getValue(),
            getValueTimestamp()
        );
    }

    /**
     * Set the attributes' value and updates the value timestamp to event time.
     *
     * @throws IllegalArgumentException If a constraint is violated.
     */
    public Attribute applyStateEvent(AttributeEvent attributeEvent) throws IllegalArgumentException {
        if (!attributeEvent.getAttributeState().getAttributeRef().getAttributeName().equals(getName())) {
            throw new IllegalArgumentException("Attribute name mismatch, expected: " + getName());
        }
        return setValue(attributeEvent.getAttributeState().getValue(), attributeEvent.getTimestamp());
    }

    /* ############################################################################### */

    // The following methods are only applicable to attributes of assets, but they are very convenient here...

    public boolean hasMetaItem(AssetMeta assetMeta) {
        return hasMetaItem(assetMeta.getName());
    }

    public MetaItem firstMetaItem(AssetMeta assetMeta) {
        return firstMetaItem(assetMeta.getName());
    }

    public boolean isProtected() {
        return hasMetaItem(PROTECTED) && firstMetaItem(PROTECTED).isValueTrue();
    }

    public boolean isReadOnly() {
        return hasMetaItem(READ_ONLY) && firstMetaItem(READ_ONLY).isValueTrue();
    }

    public boolean isStoreDatapoints() {
        return hasMetaItem(STORE_DATA_POINTS) && firstMetaItem(STORE_DATA_POINTS).isValueTrue();
    }

    /* ############################################################################### */

}
