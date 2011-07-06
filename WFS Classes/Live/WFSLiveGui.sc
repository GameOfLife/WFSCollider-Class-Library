WFSLiveGui {

	classvar <window, <views, <ctrl, <trackCtrls, <selected;

	*startup {

		var routerGui, cview, font;

		if( window.notNil ){
			window.front
		} {
			if( WFSLive.isStarted.not ) {
				WFSLive.startup
			};
			
			font = Font( Font.defaultSansFace, 12 );


			//View
			window = Window( "wfs live", Rect( 600,100, 388, 670 ) ).front;
			window.onClose_({
				ctrl.remove;
				trackCtrls.do( _.remove );
				WFSLive.free;
				routerGui.view.window.close;
				window = nil;
			});

			window.addFlowLayout;

			views = Dictionary.new;

			views[ \start ] = SmoothButton( window, 80@20 )
				.label_([ 'play', 'stop' ])
				.hiliteColor_( Color.green )
				.action_({ |bt|
					switch( bt.value.asInt)
					{1}{
						WFSLive.start
					}
					{0}{
						WFSLive.stop
				}
				});

			views[\positions] = ScaledUserView.withSliders( window, 380@380,
					Rect( -100, -100, 200, 200 ) )
				.keepRatio_( true )
				.scale_(8)
				.resize_(5)
				.unscaledDrawFunc_({ |vw|
					var pos, conf, types, color;
					pos = vw.translateScale( WFSLive.positions.collect( _ * (1@ -1) ) );
					types = WFSLive.tracks.collect( _.type );
					conf = WFSConfiguration.default.asRect;
					conf = vw.translateScale( conf );
					
					Pen.font = font;

					Pen.width = 2;
					Pen.color = Color.blue;
					Pen.strokeRect( conf );
					
					Pen.width = 0.5;

					pos.do({ |pos, i|
						var point, scaledPoints;
						Pen.color = Color.blue( WFSLive.tracks[i].level );
						
						if( WFSLive.tracks[i].type === \plane ) {
							
							point = WFSLive.tracks[i].pos.asWFSPoint;

							scaledPoints = 2.collect{ |i| WFSPoint(0,0,0).moveAZ( point.angle + 180 , point.rho*[0.9,1.1][i] ) };
							scaledPoints = vw.translateScale( scaledPoints.collect( _.asPoint * (1@ -1) ) );
							
							Pen.arrow( scaledPoints[1], scaledPoints[0] );
							Pen.stroke;

							scaledPoints = 2.collect{ |i| point.moveAZ(point.angle + (90*[1,-1][i]),100) };
							scaledPoints = vw.translateScale( scaledPoints.collect( _.asPoint * (1@ -1) ) );
							
							Pen.moveTo( scaledPoints[0] );
							Pen.lineTo( scaledPoints[1] );
							Pen.stroke;
						};
						
						Pen.addArc( pos, 4, 0, 2pi ).fill;
						
					});
					
					Pen.color = Color.black;
					pos.do({ |pos, i|
						Pen.stringAtPoint( (i+1).asString, pos + ( 6 @ -7 ) );
					});

					if( selected.notNil ) {
						Pen.width = 2;
						Pen.color = Color.yellow;
						Pen.addArc( pos[ selected ], 4, 0, 2pi ).stroke;
					};
				})
				.mouseDownAction_({ |vw, x, y|
					var id;
					id =  WFSLive.positions.collect( _ * (1@ -1) ).detectIndex({ |pt|
						pt.dist( x@y ) < (vw.pixelScale.x * 5)
					});
					selected = id;
					vw.refresh;
				})
				.mouseMoveAction_({ |vw, x, y|
					var id;
					if( selected.notNil ) {
						WFSLive.tracks[selected].pos_( x@ (y.neg) );
					};
				});

			window.view.decorator.nextLine;
				cview = CompositeView(window,380@300)
				.resize_(7);

			cview.addFlowLayout;

			views[\levels] = { |i|
				EZSmoothSlider( cview, 30@200, " " ++ (i+1), \db )
					.value_(WFSLive.tracks[i].level)
					.action_({ |sl|
						WFSLive.tracks[i].level_(sl.value.dbamp );
					})
			}!8;

			cview.decorator.nextLine;

			views[\running] = { |i|
				SmoothButton( cview, 30@20)
					.states_([["On"],["Off",Color.red(0.8)]])
					.value_( WFSLive.tracks[i].isRunning.not.binaryValue )
					.canFocus_(false)
					.action_({ |sl|
						WFSLive.tracks[i].isRunning_(sl.value.booleanValue.not);
					})
			}!8;

			cview.decorator.nextLine;

			views[\types] = { |i|
				SmoothButton( cview, 30@20)
					.states_([["."],["/"]])
					.value_([\point,\plane].indexOf( WFSLive.tracks[i].type ) )
					.canFocus_(false)
					.action_({ |sl|
						WFSLive.tracks[i].type_([\point,\plane][sl.value])
					})
			}!8;


			//Controller
			ctrl = SimpleController( WFSLive );

			ctrl.put( \start, {
				views[ \start ].value = 1;
			});

			ctrl.put( \end, {
				views[ \start ].value = 0;
			});

			trackCtrls = WFSLive.tracks.collect{ |track,i|
				SimpleController(track)
					.put(\level, { views[\levels][i].value = track.level.ampdb;
						{ views[\positions].refresh }.defer })
					.put(\pos,  { { views[\positions].refresh }.defer })
					.put(\type, { views[\types][i].value = [\point,\plane].indexOf(track.type);
						 { views[\positions].refresh }.defer })
					.put(\isRunning, { views[\running][i].value = track.isRunning.not.binaryValue })
					.put(\all, {
						views[\levels][i].value = track.level.ampdb;
						{ views[\positions].refresh }.defer;
						views[\types][i].value = [\point,\plane].indexOf(track.type);
						views[\running][i].value = track.isRunning.not.binaryValue;
					})
			};

		}
	}
}