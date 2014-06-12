/*
 * This software is provided "AS IS" without a warranty of any kind. You use it
 * on your own risk and responsibility!!! This file is shared under BSD v3
 * license. See readme.txt and BSD3 file for details.
 */

package kendzi.josm.kendzi3d.jogl.model;

import java.util.Collections;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import kendzi.jogl.camera.Camera;
import kendzi.jogl.model.factory.MeshFactory;
import kendzi.jogl.model.factory.ModelFactory;
import kendzi.jogl.model.geometry.Model;
import kendzi.jogl.model.render.ModelRender;
import kendzi.jogl.texture.dto.TextureData;
import kendzi.jogl.texture.library.TextureLibraryStorageService;
import kendzi.josm.kendzi3d.jogl.model.export.ExportItem;
import kendzi.josm.kendzi3d.jogl.model.export.ExportModelConf;
import kendzi.josm.kendzi3d.jogl.model.tmp.AbstractWayModel;
import kendzi.josm.kendzi3d.service.MetadataCacheService;
import kendzi.josm.kendzi3d.util.ModelUtil;
import kendzi.kendzi3d.josm.model.clone.RelationCloneHeight;
import kendzi.kendzi3d.josm.model.perspective.Perspective;

import org.openstreetmap.josm.data.osm.Way;

/**
 * Fence for shapes defined as way.
 * 
 * @author Tomasz Kędziora (Kendzi)
 */
public class BarrierFence extends AbstractWayModel {

    private static final java.lang.Double FENCE_HEIGHT = 1d;

    /**
     * Hight.
     */
    private double hight;

    /**
     * Min height.
     */
    private double minHeight;

    /**
     * Model of building.
     */
    private Model model;

    /**
     * Height cloner.
     */
    private List<RelationCloneHeight> heightClone;

    /**
     * Renderer of model.
     */
    private ModelRender modelRender;

    /**
     * Metadata cache service.
     */
    private MetadataCacheService metadataCacheService;

    /**
     * Texture library service.
     */
    private TextureLibraryStorageService textureLibraryStorageService;

    /**
     * Fence constructor.
     * 
     * @param pWay
     *            way
     * @param perspective
     *            perspective
     * @param pModelRender
     *            model render
     * @param pMetadataCacheService
     *            metadata cache service
     * @param pTextureLibraryStorageService
     *            texture library service
     */
    public BarrierFence(Way pWay, Perspective perspective, ModelRender pModelRender, MetadataCacheService pMetadataCacheService,
            TextureLibraryStorageService pTextureLibraryStorageService) {
        super(pWay, perspective);

        this.modelRender = pModelRender;
        this.metadataCacheService = pMetadataCacheService;
        this.textureLibraryStorageService = pTextureLibraryStorageService;
    }

    @Override
    public void buildWorldObject() {

        if (!(this.points.size() > 1)) {
            return;
        }

        String fenceType = BarrierFenceRelation.getFenceType(this.way);

        double fenceHeight = this.metadataCacheService.getPropertitesDouble("barrier.fence_{0}.height", FENCE_HEIGHT, fenceType);

        this.hight = ModelUtil.getHeight(this.way, fenceHeight);

        this.minHeight = ModelUtil.getMinHeight(this.way, 0d);

        TextureData facadeTexture = BarrierFenceRelation.getFenceTexture(fenceType, this.way, this.textureLibraryStorageService);

        ModelFactory modelBuilder = ModelFactory.modelBuilder();

        MeshFactory meshBorder = BarrierFenceRelation.createMesh(facadeTexture.getTex0(), null, "fence_border", modelBuilder);

        BarrierFenceRelation.buildWallModel(this.points, null, this.minHeight, this.hight, 0, meshBorder, facadeTexture);

        this.model = modelBuilder.toModel();
        this.model.setUseLight(true);
        this.model.setUseTexture(true);

        this.buildModel = true;

        this.heightClone = RelationCloneHeight.buildHeightClone(this.way);
    }

    @Override
    public void draw(GL2 pGl, Camera pCamera) {

        enableTransparentText(pGl);

        // replace the quad colors with the texture
        // gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
        // GL2.GL_REPLACE);
        pGl.glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL2ES1.GL_MODULATE);

        pGl.glEnable(GL.GL_CULL_FACE);

        pGl.glPushMatrix();
        pGl.glTranslated(this.getGlobalX(), 0, -this.getGlobalY());

        try {
            this.modelRender.render(pGl, this.model);

            for (RelationCloneHeight cloner : this.heightClone) {
                for (Double height : cloner) {

                    pGl.glPushMatrix();
                    pGl.glTranslated(0, height, 0);

                    this.modelRender.render(pGl, this.model);
                    pGl.glPopMatrix();

                }
            }

        } finally {

            pGl.glPopMatrix();

            pGl.glDisable(GL.GL_CULL_FACE);
        }

        disableTransparentText(pGl);

    }

    /**
     * @param pGl
     *            FIXME move to util!
     */
    public static void enableTransparentText(GL2 pGl) {
        // do not draw the transparent parts of the texture
        pGl.glEnable(GL.GL_BLEND);
        pGl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        // don't show source alpha parts in the destination

        // determine which areas of the polygon are to be rendered
        pGl.glEnable(GL2ES1.GL_ALPHA_TEST);
        pGl.glAlphaFunc(GL.GL_GREATER, 0); // only render if alpha > 0
    }

    /**
     * @param pGl
     *            FIXME move to util!
     */
    public static void disableTransparentText(GL2 pGl) {
        pGl.glDisable(GL2ES1.GL_ALPHA_TEST);
        pGl.glDisable(GL.GL_BLEND);

    }

    @Override
    public List<ExportItem> export(ExportModelConf conf) {
        if (this.model == null) {
            buildWorldObject();
        }

        return Collections.singletonList(new ExportItem(this.model, new Point3d(this.getGlobalX(), 0, -this.getGlobalY()),
                new Vector3d(1, 1, 1)));
    }

    @Override
    public Model getModel() {
        return this.model;
    }
}
