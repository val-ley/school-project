package com.mygame;

import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

/**
 * Utility class for handling Texture operations.
 */
public class TextureUtils {

    /**
     * Traverses the given spatial (and all children) and forces
     * Nearest Neighbor filtering on all textures found.This creates the "Pixel Art" crisp look.
     * * @param spatial The root node or geometry to process (e.g. rootNode or a specific model)
     * @param spatial
     */
    public static void setNearestFilter(Spatial spatial) {
        spatial.breadthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material mat = geom.getMaterial();
                
                if (mat == null) return;

                // Iterate over every parameter in the material (BaseColor, Normal, Emissive, etc.)
                for (MatParam param : mat.getParams()) {
                    // Check if the parameter is actually a Texture
                    if (param instanceof MatParamTexture) {
                        MatParamTexture texParam = (MatParamTexture) param;
                        Texture tex = texParam.getTextureValue();
                        
                        if (tex != null) {
                            // MagFilter: How it looks when you get CLOSE (Magnified) -> Crisp edges
                            tex.setMagFilter(Texture.MagFilter.Nearest);
                            
                            // MinFilter: How it looks when FAR AWAY (Minified) -> No blurring
                            tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                        }
                    }
                }
            }
        });
    }
}