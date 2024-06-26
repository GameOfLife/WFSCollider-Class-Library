/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSPathBufferView {

	// this is not finished yet, just a copy of BufSndFileView for now
	classvar <timeMode = \seconds; // or \frames
	classvar <all;
	classvar <clipBoard;

	var <wfsPathBuffer;
	var <parent, <view, <views;
	var <>action;
	var <viewHeight = 14;

	*new { |parent, bounds, action, wfsPathBuffer|
		^super.new.init( parent, bounds, action ).value_( wfsPathBuffer ).addToAll;
	}

	*clipBoard_ { |new|
		clipBoard = new;
		this.changed( \clipBoard, new );
	}

	init { |parent, bounds, inAction|
		action = inAction;
		this.makeView( parent, bounds );
	}

	addToAll {
		all = all.add( this );
	}

	*timeMode_ { |new = \frames|
		timeMode = new.asSymbol;
		all.do( _.setTimeMode( timeMode ) );
	}

	initClass {
		all = [];
	}

	doAction { action.value( this ) }

	value { ^wfsPathBuffer }
	value_ { |newWFSPathBuffer|
		if( wfsPathBuffer != newWFSPathBuffer ) {
			newWFSPathBuffer.removeDependant( this );
			wfsPathBuffer = newWFSPathBuffer;
			newWFSPathBuffer.addDependant( this );
			this.update;
		};
	}

	update {
		if( wfsPathBuffer.notNil ) { this.setViews( wfsPathBuffer ) };
	}

	resize_ { |resize|
		view.resize = resize ? 5;
	}

	remove {
		if( wfsPathBuffer.notNil ) {
			wfsPathBuffer.removeDependant( this );
		};
		all.remove( this );
	}

	setViews { |inWFSPathBuffer|
		var rect;
		// views[ \name ].value = inWFSPathBuffer.name ? "";
		views[ \filePath ].value = inWFSPathBuffer.filePath;
		views[ \loop ].value = inWFSPathBuffer.loop.binaryValue;
		views[ \rate ].value = inWFSPathBuffer.rate;
		views[ \delay ].value = inWFSPathBuffer.delay;
		if( inWFSPathBuffer.wfsPath.isWFSPath2 ) {
			views[ \miniPlot ].value = inWFSPathBuffer.wfsPath;
			views[ \startSecond ].value = inWFSPathBuffer.startSecond;
			{
				if( inWFSPathBuffer.wfsPath.asWFSPath2.notNil ) {
					views[ \dur ].string = "% (% pts)"
						.format(
							((inWFSPathBuffer.wfsPath.duration - inWFSPathBuffer.startSecond)
								/ inWFSPathBuffer.rate).asSMPTEString(1000),
							inWFSPathBuffer.wfsPath.size - inWFSPathBuffer.startFrame
						);
				} {
					views[ \dur ].string = "--:--:--:--- (- pts)";
				};
			}.defer;
		};
		views[ \startFrame ].value = inWFSPathBuffer.startFrame;
		this.setWriteButtonColor( inWFSPathBuffer );
	}

	setWriteButtonColor { |inWFSPathBuffer|
		if( inWFSPathBuffer.wfsPath.dirty ) {
			views[ \write ].states_([ [ "write data", nil, Color.red.alpha_(0.25) ] ])
		} {
			views[ \write ].states_([ ["write data"] ])
		};

	}

	setTimeMode { |mode = \seconds|
		switch ( ( mode.asString[0] ? $f ).toLower,
			$s, { // \seconds
				views[ \startSecond ].visible_( true );
				views[ \startFrame ].visible_( false );
				{ views[ \timeMode ].string = " s"; }.defer;

			},
			$f, { // \frames
				views[ \startSecond ].visible_( false );
				views[ \startFrame ].visible_( true );
				{ views[ \timeMode ].string = " pt"; }.defer;
			}
		);
	}

	setFont { |font|
		font = font ??
			{ RoundView.skin !? { RoundView.skin.font } } ??
			{ Font( Font.defaultSansFace, 10 ) };

		{
			views[ \fileLabel ].font = font;
			views[ \timeMode ].font = font;
		}.defer;

		views[ \loop ].font = font;
		//views[ \plot ].font = font;
		views[ \edit ].font = font;
		views[ \write ].font = font;
		views[ \read ].font = font;
		views[ \filePath ].font = font;
		views[ \startFrame ].font = font;
		views[ \startSecond ].font = font;
		views[ \startLabel ].font = font;
		views[ \rate ].font = font;
		views[ \delayLabel ].font = font;
		views[ \delay ].font = font;

	}

	performWFSPathBuffer { |selector ...args|
		if( wfsPathBuffer.notNil ) {
			^wfsPathBuffer.perform( selector, *args );
		} {
			^nil;
		};
	}

	*viewNumLines { ^7 }

	makeView { |parent, bounds, resize|
		var ctrl;

		ctrl = SimpleController( this.class );

		if( bounds.isNil ) { bounds= 350 @ (this.class.viewNumLines * (viewHeight + 4)) };

		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.asView.bounds;
		view.onClose_({
			ctrl.remove;
			this.remove;
		}).resize_( resize ? 5 );
		views = ();

		views[ \miniPlot ] = WFSPathBox( view, ((viewHeight * 2) + 4).asPoint )
			.action_({ |vw|
				this.performWFSPathBuffer( \wfsPath_, vw.wfsPath );
				this.setViews( wfsPathBuffer );
			});

		views[ \buttonComp ] = CompositeView( view, 148@ ((viewHeight * 2) + 4) );
		views[ \buttonComp ].addFlowLayout( 0@0, 4@4 );

		views[ \edit ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 2 )
			.label_( "edit" )
			.action_({ |bt|
				views[ \miniPlot ].openEditor;
			});

		/*
		views[ \plot ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 2 )
			.label_( "plot" )
			.action_({ |bt|
				WFSPathView( )
					.path_( wfsPathBuffer.wfsPath )
					.editMode_( \lock )
					.mouseMode_( \zoom );
			});
		*/

		views[ \write ] = SmoothButton( views[ \buttonComp ], 60 @ viewHeight )
			.radius_( 2 )
			.label_( "write data" )
			.action_({ |bt|
				ULib.savePanel({ |path|
				  	this.performWFSPathBuffer( \writeFile, nil, path );
				  	this.performWFSPathBuffer( \changed, \filePath );
				});
			});

		views[ \copy ] = SmoothButton(  views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 2 )
			.label_( "copy" )
			.action_({ |bt|
			    this.class.clipBoard = wfsPathBuffer.wfsPath.deepCopy;
			});

		views[ \buttonComp ].decorator.nextLine;

		views[ \loop ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 2 )
			.label_( [ "loop", "loop" ] )
		    .hiliteColor_( Color.green.alpha_(0.7) )
			.action_({ |bt|
				this.performWFSPathBuffer( \loop_ , bt.value.booleanValue );
				action.value( this );
			});

		views[ \read ] = SmoothButton( views[ \buttonComp ], 60 @ viewHeight )
			.radius_( 2 )
			.label_( "read data" )
			.action_({ |bt|
				ULib.openPanel({ |pth|
					var sf, wfspath, fa;
					sf = SoundFile.openRead( pth );
					if( sf.notNil ) {
						if( sf.numChannels == 9 ) {
							fa = FloatArray.newClear( sf.numFrames * sf.numChannels );
							sf.readData( fa );
							wfspath = WFSPath2.fromBufferArray( fa );
							wfspath.name = pth.basename.removeExtension;
							wfspath.filePath = pth;
							wfspath.savedCopy = wfspath.deepCopy;
							this.performWFSPathBuffer( \filePath_ , pth );
							this.performWFSPathBuffer( \wfsPath_ , wfspath );
							if( wfsPathBuffer.notNil ) {
								this.setViews( wfsPathBuffer );
							};
							action.value( this );
						} {
							"wrong number of channels (% instead of 9)\n"
								.postf( sf.numChannels );
						};
						sf.close;
					} {
						"could not read file '%'\n".postf( pth );
					};
				});
			});

		views[ \paste ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 2 )
			.label_( "paste" )
		    .action_({ |bt|
			    if( clipBoard.notNil ) {
				    this.performWFSPathBuffer( \wfsPath_, clipBoard.deepCopy );
			    };
		    });

		if( clipBoard.isNil ) {
			views[ \paste ].enabled_( false );
		};

		ctrl.put( \clipBoard, { views[ \paste ].enabled_( clipBoard.notNil ); });

		view.view.decorator.nextLine;

		views[ \fileLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.align_( \left )
			.string_( "file" );

		views[ \filePath ] = FilePathView( view,
			(bounds.width - (30 + 4)) @ viewHeight )
			.resize_( 2 )
		    .allowEmpty_( true )
			.action_({ |fv|
			    if( fv.value.size == 0 ) {
				    this.performWFSPathBuffer( \filePath_ , nil );
				    fv.value = nil;
				    action.value( this );
				} {
				    this.performWFSPathBuffer( \filePath_ , fv.value );
				    action.value( this );
			    };
			});

		views[ \durLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.align_( \left )
			.string_( "dur" );

		views[ \dur ] = StaticText( view, (bounds.width - 34) @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "--:--:--:--- (- pts)" );

		views[ \startLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.align_( \left )
			.string_( "offset" );

		views[ \startComp ] = CompositeView( view, (bounds.width - 82) @ viewHeight )
			.resize_( 2 );

		views[ \startSecond ] = SMPTEBox( views[ \startComp ],
				views[ \startComp ].bounds.moveTo(0,0) )
			.applySmoothSkin
		    .applySkin( RoundView.skin )
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performWFSPathBuffer( \startSecond_ , nb.value );
				action.value( this );
			});

		views[ \startFrame] = SmoothNumberBox( views[ \startComp ],
				views[ \startComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.clipLo_( 0 )
			.scroll_step_( 0.1 )
			.action_({ |nb|
				this.performWFSPathBuffer( \startFrame_ , nb.value );
				action.value( this );
			})
			.visible_( false );

		views[ \timeMode ] = StaticText( view, 44 @ viewHeight )
		.applySkin( RoundView.skin )
		.string_( ( \seconds: " s", \frames: " pt" )[ this.class.timeMode ] )
		.background_( Color.white.alpha_( 0.25 ) )
		.resize_( 3 )
		.mouseDownAction_({
			var actions, selected;
			actions = [ \seconds, \frames ].collect({ |item|
				MenuAction( item.asString, {
					this.class.timeMode = item;
				}).enabled_( this.class.timeMode != item );
			});
			selected = actions.detect(_.enabled.not);
			Menu( *actions ).front( QtGUI.cursorPosition - (20@0), action: selected );
		});

		views[ \rate ] = EZSmoothSlider( view, bounds.width@viewHeight,
				"rate", [ 0.125, 8, \exp, 0.125, 1 ].asSpec, 1 )
			.labelWidth_( 30 )
			.action_({ |sl|
				this.performWFSPathBuffer( \rate_ , sl.value );
				action.value( this );
			});

		views[ \rate ].sliderView.centered_(true);
		views[ \rate ].labelView.align_( \left );

		views[ \delayLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.align_( \left )
			.string_( "delay" );

		views[ \delay ] = SMPTEBox( view, (bounds.width - 82) @ viewHeight )
			.applySmoothSkin
		    .applySkin( RoundView.skin )
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performWFSPathBuffer( \delay_ , nb.value );
				action.value( this );
			});

		this.setFont;
	}

}
