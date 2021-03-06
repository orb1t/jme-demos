package com.jmedemos.stardust.scene;

import java.net.URL;
import java.util.logging.Logger;

import com.jme.bounding.BoundingSphere;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmedemos.stardust.util.ShaderUtils;

@SuppressWarnings("serial")
public class Planet extends Sphere {
    private Logger log = Logger.getLogger(Planet.class.getName());
    private TextureState planetTextures;
    private boolean useShader = false; 
    
    private float cloudHeight = 0.004f;
    
    private float atmoGlowPower = 20f;
    private float atmoAbsPower  = 2f;
    private float atmoDensity   = 1f;
    private ColorRGBA atmoColor = new ColorRGBA(0.7f, 0.8f, 1.0f, 1.0f);
    
    private static GLSLShaderObjectsState planetShader;
    private static GLSLShaderObjectsState atmoShader;
    private static CullState backCull;
    private static CullState frontCull;
    private static BlendState blendAlpha;
    private static MaterialState atmoMaterial;
    private static MaterialState planetMaterial;

    public Planet(String texName, float radius, boolean useShader){
        super(texName, 64, 64, radius);
        log.info("Loaded planet model");
        this.useShader = useShader;
        Renderer r = DisplaySystem.getDisplaySystem().getRenderer();
        
        if (planetShader == null && useShader == true) {
            planetShader = ShaderUtils.getPlanetShader();
            atmoShader = ShaderUtils.getAtmosphereShader();

            backCull = r.createCullState();
            backCull.setCullFace(CullState.Face.Back);
            
            frontCull = r.createCullState();
            backCull.setCullFace(CullState.Face.Front);

            blendAlpha = r.createBlendState();
            blendAlpha.setBlendEnabled(true);
            blendAlpha.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blendAlpha.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            
            atmoMaterial = r.createMaterialState();
            atmoMaterial.setMaterialFace(MaterialState.MaterialFace.FrontAndBack);
            atmoMaterial.setColorMaterial(MaterialState.ColorMaterial.AmbientAndDiffuse);
            
            planetMaterial = r.createMaterialState();
            planetMaterial.setMaterialFace(MaterialState.MaterialFace.Front);
            planetMaterial.setShininess(32f);
        }
        
        Texture normalmap = null;
        Texture specmap = null;
        URL normal;
        URL spec;
        URL color  = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, texName+".png");
        if (useShader) {
            normal = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, texName+"_normal.png");
            spec   = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, texName+"_specular.png");
            normalmap = TextureManager.loadTexture(normal,
                    Texture.MinificationFilter.BilinearNoMipMaps, 
                    Texture.MagnificationFilter.Bilinear, 0.0f, false);
            specmap   = TextureManager.loadTexture(spec,
                    Texture.MinificationFilter.BilinearNoMipMaps, 
                    Texture.MagnificationFilter.Bilinear, 0.0f, false);
        }
      
        Texture colormap  = TextureManager.loadTexture(color,
                Texture.MinificationFilter.BilinearNoMipMaps, 
                Texture.MagnificationFilter.Bilinear, 0.0f, false);

        log.info("Loaded planet textures");
        getLocalRotation().fromAngles(FastMath.HALF_PI, 0f, 0f);
        
        planetTextures = r.createTextureState();
        planetTextures.setTexture(colormap, 0);
        if (useShader) {
            planetTextures.setTexture(normalmap, 1);
            planetTextures.setTexture(specmap, 2);
//            planetTextures.setTexture(cloudmap, 3);
        }
        
        planetTextures.load();
        setVBOInfo(new VBOInfo(true));
//        lockBounds();
//        self.lockTransforms();
        setModelBound(new BoundingSphere());
        updateModelBound();
        if (useShader == false) {
        	setRenderState(planetTextures);
        }
    }
    
    public void updateUniforms(){
        atmoShader.setUniform("fCloudHeight", cloudHeight);
        atmoShader.setUniform("fvAtmoColor",  atmoColor.r, atmoColor.g, atmoColor.b, atmoColor.a);
        atmoShader.setUniform("fAtmoDensity", atmoDensity);
        atmoShader.setUniform("fAbsPower",    atmoAbsPower);
        atmoShader.setUniform("fGlowPower",   atmoGlowPower);
    }
    
    public void renderShader(Renderer r) {
    	updateUniforms();
    	
    	clearRenderState(RenderState.RS_TEXTURE);
    	setRenderState(atmoShader);
    	setRenderState(blendAlpha);
    	setRenderState(atmoMaterial);
    	
    	// back of atmosphere
    	setRenderState(frontCull);
    	updateRenderState();
    	r.draw(this);
    	
    	clearRenderState(RenderState.RS_BLEND);
    	clearRenderState(RenderState.RS_CULL);
    	
    	// PLANET
    	setRenderState(planetTextures);
    	setRenderState(planetShader);
    	setRenderState(planetMaterial);
    	
    	updateRenderState();
    	r.draw(this);
    	
    	clearRenderState(RenderState.RS_TEXTURE);
    	setRenderState(atmoShader);
    	setRenderState(blendAlpha);
    	setRenderState(atmoMaterial);
    	
    	// front of atmosphere
    	setRenderState(backCull);
    	updateRenderState();
    }
    
    @Override
    public void draw(Renderer r){
        if (useShader)
            renderShader(r);
        
        r.draw(this);
    }
            
}
