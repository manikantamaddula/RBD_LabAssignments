/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 
 */
package org.openimaj.demos.video.utils;

import Jama.Matrix;
import org.openimaj.demos.faces.Mustache;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.MatchingUtilities;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.FacialKeypoint;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.renderer.MBFImageRenderer;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.MatrixTransformProvider;
import org.openimaj.math.geometry.transforms.check.TransformMatrixConditionCheck;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.openimaj.image.ImageUtilities.readMBF;

/**
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * 
 * @created 28 Sep 2011
 */
public class ProcessingPanel extends JPanel
		implements VideoDisplayListener<MBFImage>
{
	/** */
	private static final long serialVersionUID = 1L;


    enum RenderMode {
        SQUARE {

            public void render(final MBFImageRenderer renderer, final Matrix transform, final org.openimaj.math.geometry.shape.Rectangle rectangle) {
                renderer.drawShape(rectangle.transform(transform), 3, RGBColour.BLUE);
            }
        }
        }

	private boolean edgeDetect = false;
	private boolean faceDetect = false;
	private boolean faceKPDetect = false;
	private boolean moustache = false;
	private boolean balltracking = false;
    public MBFImage modelImage2;
    private ConsistentLocalFeatureMatcher2d<Keypoint> matcher;
    public DoGSIFTEngine engine;
    private MBFImage currentFrame;
	private final FKEFaceDetector fkp;
    private RenderMode renderMode = RenderMode.SQUARE;

	private final HaarCascadeDetector d;

	/**
	 * 
	 */
	public ProcessingPanel()
	{
		this.d = new HaarCascadeDetector(100);
		this.fkp = new FKEFaceDetector(
				HaarCascadeDetector.BuiltInCascade.frontalface_alt.load());
		this.init();
	}

	/**
	 * 
	 */
	private void init()
	{
		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder("Processing"));

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;

		// -----------------------------------------------------
		final JCheckBox edgeDetectButton = new JCheckBox("Edge Detect", this.edgeDetect);
		edgeDetectButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				ProcessingPanel.this.edgeDetect = edgeDetectButton.isSelected();
			}
		});
		gbc.gridy++;
		this.add(edgeDetectButton, gbc);

		// -----------------------------------------------------
		final JCheckBox faceDetectorButton = new JCheckBox("Face Detection", this.faceDetect);
		faceDetectorButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				ProcessingPanel.this.faceDetect = faceDetectorButton.isSelected();
			}
		});
		gbc.gridy++;
		this.add(faceDetectorButton, gbc);

		// -----------------------------------------------------
		final JCheckBox faceKPDetectorButton = new JCheckBox("Facial Keypoint Detection", this.faceKPDetect);
		faceKPDetectorButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				ProcessingPanel.this.faceKPDetect = faceKPDetectorButton.isSelected();
			}
		});
		gbc.gridy++;
		this.add(faceKPDetectorButton, gbc);

		// -----------------------------------------------------
		final JCheckBox moustacheButton = new JCheckBox("Add Moustaches", this.moustache);
		moustacheButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				ProcessingPanel.this.moustache = moustacheButton.isSelected();
			}
		});
		gbc.gridy++;
		this.add(moustacheButton, gbc);

		// -----------------------------------------------------
		final JCheckBox trackingButton = new JCheckBox("Add ball tracking", this.balltracking);
		trackingButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				ProcessingPanel.this.balltracking = trackingButton.isSelected();
			}
		});
		gbc.gridy++;
		this.add(trackingButton, gbc);



	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openimaj.video.VideoDisplayListener#afterUpdate(org.openimaj.video.VideoDisplay)
	 */
	@Override
	public void afterUpdate(final VideoDisplay<MBFImage> display)
	{
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openimaj.video.VideoDisplayListener#beforeUpdate(org.openimaj.image.Image)
	 */
	@Override
	public void beforeUpdate(final MBFImage frame)
	{
		if (this.edgeDetect)
			frame.processInplace(new CannyEdgeDetector());

		if (this.faceDetect)
		{
			final List<DetectedFace> faces = this.d.detectFaces(
					Transforms.calculateIntensityNTSC(frame));

			for (final DetectedFace face : faces)
			{
				final Shape transBounds = face.getBounds();
				final MBFImageRenderer renderer = frame.createRenderer();
				renderer.drawPolygon(transBounds.asPolygon(), RGBColour.CYAN);
			}

		}

		if (this.faceKPDetect)
		{
			final List<KEDetectedFace> faces = this.fkp.detectFaces(
					Transforms.calculateIntensityNTSC(frame));

			for (final KEDetectedFace face : faces)
			{
				final Shape transBounds = face.getBounds();
				final MBFImageRenderer renderer = frame.createRenderer();
				renderer.drawPolygon(transBounds.asPolygon(), RGBColour.RED);

				for (final FacialKeypoint kp : face.getKeypoints())
				{
					final Point2d pt = kp.position.clone();
					pt.translate((float) transBounds.minX(), (float) transBounds.minY());
					renderer.drawPoint(pt, RGBColour.GREEN, 3);
				}
			}
		}

		if (this.moustache)
			try {
				frame.internalAssign(new Mustache().addMustaches(frame));
			} catch (final IOException e) {
				e.printStackTrace();
			}

        if (this.balltracking)
        {try {
                //final Polygon p = this.polygonListener.getPolygon().clone();
                //this.polygonListener.reset();
                //System.out.println("woah..........");
                //System.out.println(frame.flatten());
            //MBFImage image1 = readMBF(new File("C:\\Users\\Manikanta\\Pictures\\Camera Roll\\WIN_20160922_13_30_58_Pro_2.jpg"));
            //MBFImage image1 = readMBF(new File("C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\RBD\\football_official.jpeg"));
            MBFImage image1 = readMBF(new File("C:\\Users\\Manikanta\\Documents\\UMKC Subjects\\RBD\\neymar_jersey_2.jpg"));

                this.modelImage2=image1;

                //this.modelImage = this.currentFrame.process(image1);


                    final RobustHomographyEstimator ransac = new RobustHomographyEstimator(0.1, 100,
                            new RANSAC.PercentageInliersStoppingCondition(0.1), HomographyRefinement.NONE,
                            new TransformMatrixConditionCheck<HomographyModel>(100));

                    this.matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                            new FastBasicKeypointMatcher<Keypoint>(4));
                    this.matcher.setFittingModel(ransac);



                //this.modelFrame.setImage(createBufferedImageForDisplay(this.modelImage2));

                DoGSIFTEngine engine = new DoGSIFTEngine();
                engine.getOptions().setDoubleInitialImage(true);

                // final ASIFTEngine engine = new ASIFTEngine(true);

                //final FImage modelF = Transforms.calculateIntensityNTSC(this.modelImage);
                final FImage modelF = Transforms.calculateIntensityNTSC(this.modelImage2);
                this.matcher.setModelFeatures(engine.findFeatures(modelF));

            final MBFImage capImg = frame;
            System.out.println(frame.flatten());
            System.out.println(Transforms.calculateIntensityNTSC(frame).getPixel(1,1));

            final FImage testF=Transforms.calculateIntensityNTSC(frame);
            final LocalFeatureList<Keypoint> kpl = engine.findFeatures(testF);

            final MBFImageRenderer renderer = capImg.createRenderer();
            renderer.drawPoints(kpl, RGBColour.MAGENTA, 3);

            this.matcher.findMatches(kpl);
            MBFImage matches;
                try {
                    final Matrix boundsToPoly = ((MatrixTransformProvider) this.matcher.getModel()).getTransform()
                            .inverse();

                    if (modelImage2.getBounds().transform(boundsToPoly).isConvex()) {
                        //this.renderMode.render(renderer, boundsToPoly, this.modelImage2.getBounds());
                    }
                } catch (final RuntimeException e) {
                }

                matches = MatchingUtilities
                        .drawMatches(this.modelImage2, capImg, this.matcher.getMatches(), RGBColour.RED);
            this.matcher.getAllMatches();

                //frame.setImage(createBufferedImageForDisplay(matches));

            System.out.println(matches.flatten());

            //System.out.println("working here don't disturb");


            //this.matchPanel.setPreferredSize(this.matchPanel.getSize());
            //this.matchFrame.setImage(createBufferedImageForDisplay(matches));
            org.openimaj.math.geometry.shape.Polygon p=new Polygon();
            MBFImageRenderer renderer2 = frame.createRenderer();
            //renderer2.drawImage(this.modelImage2, 10, 10);
            renderer2.drawImage(matches,0,0);


            //renderer2.drawPolygon(p, RGBColour.RED);
            //renderer2.drawPolygon(matches.asPolygon(), RGBColour.RED);

            //frame.drawText("ball",1,1,f,10);
            String x="ball here";
            //renderer2.drawText(x, 1, 1,, 10);

        } catch (final Exception e) {
                e.printStackTrace();
            }

         }



    }
}
