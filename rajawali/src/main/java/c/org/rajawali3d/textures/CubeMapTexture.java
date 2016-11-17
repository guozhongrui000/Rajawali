/**
 * Copyright 2013 Dennis Ippel
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package c.org.rajawali3d.textures;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import net.jcip.annotations.ThreadSafe;

import org.rajawali3d.util.RajLog;

import c.org.rajawali3d.gl.extensions.EXTTextureFilterAnisotropic;
import c.org.rajawali3d.textures.annotation.Filter;
import c.org.rajawali3d.textures.annotation.Type;
import c.org.rajawali3d.textures.annotation.Wrap;
import c.org.rajawali3d.textures.annotation.Wrap.WrapType;

/**
 * A 2D cube mapped environmental texture. These textures are typically used to simulate highly reflective
 * surfaces by providing what the reflected environment would look like. For static or basic reflective appearances,
 * a single texture can be used. For more advanced reflections, the scene can be rendered to a FBO with cube
 * mapping which is used as a {@link Type#CUBE_MAP} texture. They are also commonly used for sky boxes, simulating
 * the appearance of a far off sky.
 *
 * @author dennis.ippel
 * @author Jared Woolston (Jared.Woolston@gmail.com)
 */
@SuppressWarnings("WeakerAccess")
@ThreadSafe
public class CubeMapTexture extends MultiTexture2D {

    public final int[] CUBE_FACES = new int[]{
        GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
        GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
        GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
        GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
        GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
        GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
    };

    private boolean isSkyTexture = false;

    /**
     * Constructs a new {@link CubeMapTexture} with data and settings from the provided {@link CubeMapTexture}.
     *
     * @param other The other {@link CubeMapTexture}.
     */
    public CubeMapTexture(@NonNull CubeMapTexture other) throws TextureException {
        super(other);
    }

    /**
     * Constructs a new {@link CubeMapTexture} with the provided name and no data.
     *
     * @param name {@link String} The texture name.
     */
    public CubeMapTexture(@NonNull String name) {
        super(Type.CUBE_MAP, name);
        setWrapType((Wrap.CLAMP_S | Wrap.CLAMP_T));
        setTextureTarget(GLES20.GL_TEXTURE_CUBE_MAP);
    }

    /**
     * Constructs a new {@link CubeMapTexture} with data provided by the Android resource id. The texture name is set
     * by querying Android for the resource name.
     *
     * @param name        {@link String} The texture name.
     * @param context     {@link Context} The application context.
     * @param resourceIds {@code int} The Android resource id to load from.
     */
    public CubeMapTexture(@NonNull String name, @NonNull Context context,
                          @NonNull @DrawableRes @Size(6) int[] resourceIds) {
        super(Type.CUBE_MAP, name, context, resourceIds);
        setWrapType((Wrap.CLAMP_S | Wrap.CLAMP_T));
        setTextureTarget(GLES20.GL_TEXTURE_CUBE_MAP);
    }

    /**
     * Constructs a new {@link CubeMapTexture} with the provided data.
     *
     * @param name {@link String} The texture name.
     * @param data {@link TextureDataReference}[] The texture data.
     */
    public CubeMapTexture(@NonNull String name, @NonNull @Size(6) TextureDataReference[] data) {
        super(Type.CUBE_MAP, name, data);
        setWrapType(Wrap.CLAMP_S | Wrap.CLAMP_T);
        setTextureTarget(GLES20.GL_TEXTURE_CUBE_MAP);
    }

    /**
     * Sets whether or not this texture is treated a sky sphere or environmental map. By default,
     * {@link CubeMapTexture}s are treated as environmental maps.
     *
     * @param value {@code true} if this texture should be treated as a sky sphere.
     */
    public void isSkyTexture(boolean value) {
        isSkyTexture = value;
    }

    /**
     * Returns whether or not this texture is treated as a sky sphere or environmental map. By default,
     * {@link CubeMapTexture}s are treated as environmental maps.
     *
     * @return {@code true} if this texture is treated as a sky sphere.
     */
    public boolean isSkyTexture() {
        return isSkyTexture;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public CubeMapTexture clone() {
        try {
            return new CubeMapTexture(this);
        } catch (TextureException e) {
            RajLog.e(e.getMessage());
            return null;
        }
    }

    @Size(6)
    @NonNull
    @Override
    public TextureDataReference[] setTextureDataFromResourceIds(@NonNull Context context,
                                                                @DrawableRes @Size(6) int[] resourceIds) {
        return super.setTextureDataFromResourceIds(context, resourceIds);
    }

    @Override
    public void setTextureData(@Nullable @Size(6) TextureDataReference[] data) {
        super.setTextureData(data);
    }

    @Size(6)
    @Nullable
    @Override
    protected TextureDataReference[] getTextureData() {
        return super.getTextureData();
    }

    @Override
    void add() throws TextureException {
        final TextureDataReference[] dataReferences = getTextureData();

        if (dataReferences == null) {
            throw new TextureException("Texture data was null!");
        }

        if (dataReferences.length < 6) {
            throw new TextureException("Texture data was of insufficient length. Was: " + dataReferences.length
                                       + " Expected: 6");
        }

        for (int i = 0; i < 6; ++i) {
            if (dataReferences[i] == null || dataReferences[i].isDestroyed()
                || (dataReferences[i].hasBuffer() && dataReferences[i].getByteBuffer().limit() == 0
                    && !dataReferences[i].hasBitmap())) {
                throw new TextureException("Texture could not be added because there is no valid data set.");
            }
        }

        @Filter.FilterType final int filterType = getFilterType();
        @WrapType final int wrapType = getWrapType();

        // Generate a texture id
        int[] genTextureNames = new int[1];
        GLES20.glGenTextures(1, genTextureNames, 0);
        int textureId = genTextureNames[0];

        if (textureId > 0) {
            // If a valid id was generated...
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId);

            // Handle minification filtering
            if (isMipmaped()) {
                switch (filterType) {
                    case Filter.NEAREST:
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_NEAREST_MIPMAP_NEAREST);
                        break;
                    case Filter.BILINEAR:
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_LINEAR_MIPMAP_NEAREST);
                        break;
                    case Filter.TRILINEAR:
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_LINEAR_MIPMAP_LINEAR);
                        break;
                    default:
                        throw new TextureException("Unknown texture filtering mode: " + filterType);
                }
            } else {
                switch (filterType) {
                    case Filter.NEAREST:
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_NEAREST);
                        break;
                    case Filter.BILINEAR:
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_LINEAR);
                        break;
                    case Filter.TRILINEAR:
                        RajLog.e("Trilinear filtering requires the use of mipmaps which are not enabled for this "
                                 + "texture. Falling back to bilinear filtering.");
                        GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER,
                                               GLES20.GL_LINEAR);
                        break;
                    default:
                        throw new TextureException("Unknown texture filtering mode: " + filterType);
                }
            }

            // Handle magnification filtering
            if (filterType == Filter.BILINEAR || filterType == Filter.TRILINEAR) {
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            } else {
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            }

            // Handle anisotropy if needed. We don't check if it is supported here because setting it to anything
            // other than 1.0 would have required the check.
            if (getMaxAnisotropy() > 1.0) {
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_CUBE_MAP, EXTTextureFilterAnisotropic.TEXTURE_MAX_ANISOTROPY_EXT,
                                       getMaxAnisotropy());
            }

            // Handle s coordinate wrapping
            int wrap = GLES20.GL_REPEAT;
            if ((wrapType & Wrap.CLAMP_S) != 0) {
                wrap = GLES20.GL_CLAMP_TO_EDGE;
            } else if ((wrapType & Wrap.MIRRORED_REPEAT_S) != 0) {
                wrap = GLES20.GL_MIRRORED_REPEAT;
            }
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, wrap);

            // Handle t coordinate wrapping
            wrap = GLES20.GL_REPEAT;
            if ((wrapType & Wrap.CLAMP_T) != 0) {
                wrap = GLES20.GL_CLAMP_TO_EDGE;
            } else if ((wrapType & Wrap.MIRRORED_REPEAT_T) != 0) {
                wrap = GLES20.GL_MIRRORED_REPEAT;
            }
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, wrap);

            for (int i = 0; i < 6; i++) {
                GLES20.glHint(GLES20.GL_GENERATE_MIPMAP_HINT, GLES20.GL_NICEST);
                if (dataReferences[i].hasBuffer()) {
                    if (getWidth() == 0 || getHeight() == 0) {
                        throw new TextureException(
                                "Could not create ByteBuffer texture. One or more of the following properties haven't "
                                + "been set: width or height format");
                    }
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP, 0, getTexelFormat(), getWidth(), getHeight(), 0,
                                        dataReferences[i].getPixelFormat(), dataReferences[i].getDataType(),
                                        dataReferences[i].getByteBuffer());
                } else {
                    GLUtils.texImage2D(CUBE_FACES[i], 0, dataReferences[i].getBitmap(), 0);
                }
            }

            if (isMipmaped()) {
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
            }

            // Store the texture id
            setTextureId(textureId);
        } else {
            throw new TextureException("Failed to generate a new texture id.");
        }

        if (willRecycle()) {
            setTextureData(null);
        }

        // Rebind the null texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, 0);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    void replace() throws TextureException {
        final TextureDataReference[] textureData = getTextureData();

        if (textureData == null) {
            final String error = "CubeMapTexture could not be replaced because the data is null.";
            RajLog.e(error);
            throw new TextureException(error);
        }

        for (int i = 0, j = textureData.length; i < j; ++i) {
            if (textureData[i].isDestroyed() || (textureData[i].hasBuffer()
                                                 && textureData[i].getByteBuffer().limit() == 0
                                                 && !textureData[i].hasBitmap())) {
                final String error = "CubeMapTexture could not be replaced because the data is empty.";
                RajLog.e(error);
                throw new TextureException(error);
            }
            if (textureData[i].getWidth() != getWidth() || textureData[i].getHeight() != getHeight()) {
                throw new TextureException(
                        "Texture could not be updated because the texture size is different from the original.");
            }
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, getTextureId());

        for (int i = 0, j = textureData.length; i < j; ++i) {
            if (textureData[i].hasBuffer()) {
                GLES20.glTexSubImage2D(CUBE_FACES[i], 0, 0, 0, getWidth(), getHeight(),
                                       textureData[i].getPixelFormat(),
                                       GLES20.GL_UNSIGNED_BYTE, textureData[i].getByteBuffer());
            } else {
                int bitmapFormat = textureData[i].getBitmap().getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA
                                                                                           : GLES20.GL_RGB;

                if (bitmapFormat != getTexelFormat()) {
                    throw new TextureException(
                            "Texture could not be updated because the texel format is different from the original");
                }

                GLUtils.texSubImage2D(CUBE_FACES[i], 0, 0, 0, textureData[i].getBitmap(), getTexelFormat(),
                                      GLES20.GL_UNSIGNED_BYTE);
            }
        }

        if (isMipmaped()) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, 0);
    }
}