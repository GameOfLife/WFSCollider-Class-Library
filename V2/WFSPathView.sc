WFSBasicEditView {
	
	var <object, <view;
	
	var <selected, <allSelected = false;
	
	var <selectRect, <hitIndex;
	var <hitPoint, <lastPoint, <optionOn = false;
	
	var <>drawMode = 0; // 0: points+lines, 1: lines, 2: points, 3: none, 4: hi-res lines
	var <>showControls = false;
	
	var <mouseMode = \select; // \select, \zoom, \move (move canvas)
	var <editMode = \move; // \move, \scale, \rotate, \rotateScale, \none
	var mouseEdit = false, externalEdit = false;
	
	var <gridColor;
	
	var <>stepSize = 0.1;
	var <>round = 0;
	
	var <undoManager;
	
	*new { |parent, bounds, object|
		^this.newCopyArgs(object).makeView( parent, bounds );
	}
	
	makeView { |parent, bounds|
		
		gridColor = gridColor ?? { Color.white.alpha_(0.25) };

		object = object ?? { WFSPath2( { (8.0@8.0).rand2 } ! 7, [0.5] ); }; // default object
		
		view = ScaledUserView.withSliders( parent, bounds, Rect(-100, -100, 200, 200 ) )
			.scaleSliderLength_( 40 )
			.sliderWidth_( 10 )
			.move_( [0.5,0.5] )
			.scale_( [10,10] )
			.maxZoom_( 20 )
			.keepRatio_( true )
			.resize_(5)
			.gridLines_( [ 200, 200 ] )
			.gridMode_( \lines )
			.gridColor_( gridColor );
			
		view.mouseDownAction = { |vw, x,y, mod, oX, oY, isInside, bn, cc|
			var scaler, includes;
			
			scaler = vw.pixelScale.asArray.mean;
			mod = ModKey(mod);
			mouseEdit = false;
			
			switch( mouseMode, 
				\select, {  
					this.changed( \mouse_down );
					hitPoint = (x@y);
					lastPoint = hitPoint;
					hitIndex = this.getNearestIndex( hitPoint * (1@ -1), scaler * 5 );
					includes = selected.asCollection.includes( hitIndex );
					if( mod.shift ) { 
						if( hitIndex.notNil ) { 
							if( includes ) { 
								selected.remove( hitIndex ); 
								this.select( selected ); 
							} { 
								this.select( selected.add( hitIndex ) ); 
							}; 
						}; 
					} { 
						if( hitIndex.notNil ) { 
							if( includes.not ) { 
								this.select( hitIndex ) 
							};
						} { 
							this.select() 
						};
					};
					
				}, \move, {
					if( cc == 2 ) { vw.movePixels = [0,0]; }; // double click
					hitPoint = vw.movePixels.asPoint - (oX@oY);
				}, \zoom, {
					if( cc == 2 ) { 
						this.zoomToFit; 
					} { 
						hitPoint = (x@y); 
					};
				}, \record, {
					this.changed( \start_record );
					selected = [];
					this.startRecord( (x@y) * (1 @ -1), true );
				});
				
			vw.refresh;
		};
		
		view.mouseMoveAction = { |vw, x,y, mod, oX, oY|
			var newPoint, pts, tms;
			mod = ModKey(mod);
			
			switch( mouseMode,
				\select, {	
					newPoint = (x@y);	
					
					// move canvas if out of bounds
					if( vw.viewRect.contains( newPoint ).not ) {
						{ 
							view.viewRect =   // change to moving/not scaling later?
								view.viewRect.union( Rect.fromPoints( newPoint, newPoint ) )
									.sect( vw.fromBounds ); 
						}.defer(0.5); // 0.4s delay 
					} {	
						if( hitIndex.isNil ) {  // no point hit -> change selection
							selectRect = Rect.fromPoints( hitPoint, newPoint ).scale(1@(-1));
							
							pts = this.getIndicesInRect( selectRect );
							
							if( mod.shift ) {
								this.addToSelection( pts );
							} { 
								this.selectNoUpdate(pts);
							}; 
							
						} { // selected point hit -> edit contents
							if( editMode != \none ) {
								if( externalEdit ) {
									this.changed( \mouse_edit );
									externalEdit = false;
								};
								if( mod.option && { optionOn.not }) { 
									this.duplicateSelected;
									optionOn = true;
								};
								mouseEdit = true;
								this.mouseEditSelected( newPoint );
							} {
								mouseEdit = false;
							};
						};
					};
					lastPoint = newPoint;
				}, \move, { view.movePixels_( hitPoint + (oX@oY) ); 
				}, \zoom, {
						if( hitPoint.notNil )
							{ 
							newPoint = (x@y);	
							if( hitPoint.dist( newPoint ) > 1 )
								{ selectRect = Rect.fromPoints( hitPoint, newPoint )
										.scale(1@(-1)); }
								{ selectRect = nil };
							};
				}, \record, {
					this.recordPoint( (x@y) * (1@ -1) );
					this.refresh;
				});
		};
		
		
		view.mouseUpAction = { |vw, x, y, mod|
				
				if( mouseMode == \zoom ) { 
					 if( hitPoint.notNil ) { 
						 if( selectRect.notNil ) { 
							 this.zoomToRect( selectRect );
						} { 
							mod = ModKey( mod );
							case { 
								mod.shift 
							} { 
								this.zoomIn; 
							} { 
								mod.ctrl 
							} {
								this.zoomOut;
							};
						};
					}; 
				};
				
				optionOn = false;
				
				if( mouseMode == \record ) { 
					this.endRecord;
					this.mouseMode = \select; 
				} {	
					if( mouseEdit ) { 
						mouseEdit = false;
						this.changed( \mouse_edit, editMode );
					};
					selectRect = nil;
					hitPoint = nil;
				};
				
				vw.refresh;
		};
		
		
		view.keyDownAction = { |vw, char, modifiers, unicode, keycode|
			var dict;
			
			if( editMode != \none ) {	
				dict = (
					127: \backspace, 
					63234: \leftArrow, 
					63235: \rightArrow,
					63232: \upArrow, 
					63233: \downArrow
				);
				 
				switch( dict[ unicode ],
					\backspace, { 
						this.removeSelected;
					},
					\leftArrow, { 
						if( selected.size == 0 ) { this.select(\all) };
						this.moveSelected( stepSize.neg, 0 )
					},
					\rightArrow, { 
						if( selected.size == 0 ) { this.select(\all) };
						this.moveSelected( stepSize, 0 )
					},
					\upArrow, { 
						if( selected.size == 0 ) { this.select(\all) };
						this.moveSelected( 0, stepSize )
					},
					\downArrow, { 
						if( selected.size == 0 ) { this.select(\all) };
						this.moveSelected( 0, stepSize.neg ) 
					}
				);
			};
		};
		
		view.drawFunc = { |vw|			
			this.drawContents(  vw.pixelScale.asArray.mean );
		};
			
		view.unscaledDrawFunc = { |vw|
			var rect;
			
			/// border
			if( vw.view.hasFocus ) { Pen.width = 3; } { Pen.width = 1 };
			Pen.color = Color.gray(0.2).alpha_(0.75);
			Pen.strokeRect( vw.drawBounds.insetBy(0.5,0.5) );
			
			//// selection
			if( selectRect.notNil ) { 
				Pen.width = 1;
				rect = selectRect.scale(1@(-1));
				rect = vw.translateScale(rect);
				switch( mouseMode,
					\select, {
					  //Pen.fillColor = selectColor.copy.alpha_(0.05); 
					  // Pen.strokeColor = selectColor.copy.alpha_(0.5); 
					  Pen.fillColor = Color.black.alpha_(0.05); 
					  Pen.strokeColor = Color.black.alpha_(0.25);
					  Pen.lineDash_( FloatArray[4, 4] );
					},
					\zoom, {
					  Pen.fillColor = Color.black.alpha_(0.05); 
					  Pen.strokeColor = Color.black.alpha_(0.25); 
					});
					
				Pen.addRect( rect ).fillStroke;
			};
		};	
	}
	
	refresh {
		if( view.view.isClosed.not ) { view.refresh };
	}
	
	undoKeys { ^[ \mouse_edit, \new_object ] }
	
	undoManager_ { |um|
		undoManager = um;
		this.changed( \new_object ); // force first undo state
	}
	
	undo { |numSteps = 1|
		var obj;
		if( undoManager.notNil ) {
			obj = undoManager.undo( numSteps );
			if( obj.notNil ) {
				object = obj;
				externalEdit = true;
				this.refresh;
				this.changed( \undo );
			};
		};
	}
	
	changed { |what ... moreArgs|
		if( undoManager.notNil ) {
			if( this.undoKeys.includes( what ) ) {
				undoManager.add( this.object, ([ what ] ++ moreArgs).join("_").asSymbol );
			};
		};
		super.changed( what, *moreArgs );
	}
	
	zoomToFit { |includeCenter = true|
		if( includeCenter ) { 
			view.viewRect_( object.asRect.scale(1@(-1))
				.union( Rect(0,0,0,0) ).insetBy(-1,-1) );  
		} { 
			view.viewRect_( object.asRect.scale(1@(-1)).insetBy(-1,-1) ); 
		};
	}
	
	zoomToRect { |rect|
		rect = rect ?? { 
			object.asRect.union( Rect(0,0,0,0) ).insetBy(-1,-1) 
		};
		view.viewRect = rect.scale(1@(-1));
	}
	
	zoomIn { |amt|
		amt = amt ?? { 2.sqrt };
		view.scale = view.scale * amt;
	}
	
	zoomOut { |amt|
		amt = amt ?? { 2.sqrt };
		view.scale = view.scale / amt;
	}
	
	zoom { |level = 1|
		view.scale = level*10;
	}
	
	move { |x,y|
		x = x ? 0;
		y = y ? x;
		view.move_([x,y].linlin(-100,100,0,1));
	}
	
	moveToCenter { 
		view.move_([0.5,0.5]);
	}
	
	object_ { |newPath| 
			object = newPath;
			this.refresh;
			this.changed( \new_object ); // update gui 
	}
	
	mouseMode_ { |newMode|
		newMode = newMode ? \select;
		mouseMode = newMode;
		this.changed( \mouseMode );
	}
	
	editMode_ { |newMode|
		newMode = newMode ? \move;
		editMode = newMode;
		this.changed( \editMode );
	}
	
	gridColor_ { |aColor|
		gridColor = aColor ?? { Color.white.alpha_(0.25) };
		view.gridColor = aColor;
	}
	
		addToSelection { |...indices|
		 this.select( *((selected ? []).asSet.addAll( indices ) ).asArray );
	}
	
	selectAll { this.select( \all ) }
	selectNone { this.select( ) }
}



WFSPathView : WFSBasicEditView {
	
	
	var <pos; 
	var <recordLastTime;
	var <animationTask, <>animationRate = 1;
	
	mouseEditSelected { |newPoint|
		var pt;
		// returns true if changed
		switch( editMode,
			\move,  { 
				pt = (newPoint.round(round) - lastPoint.round(round)) * (1@(-1));
				this.moveSelected( pt.x, pt.y, false );
			},
			\scale, { 
				pt = [ lastPoint.round(round).excess(0.001) * 
						lastPoint.asArray.collect({ |item|
							(item > 0).binaryValue.linlin(0,1,-1,1)
						}).asPoint,
					  newPoint.round(round).abs.max(0.001) * 
						newPoint.asArray.collect({ |item|
							(item > 0).binaryValue.linlin(0,1,-1,1)
						}).asPoint
				]; // prevent inf/nan
				pt = pt[1] / pt[0];
				this.scaleSelected( pt.x, pt.y, false ); 
			},
			\rotate, { 
				this.rotateSelected( 
					lastPoint.angle - newPoint.angle, 
					1, 
					false
				);
			},
			\rotateScale, { 
				this.rotateSelected( 
					lastPoint.theta - newPoint.theta, 
					newPoint.rho.max(0.001) / lastPoint.rho.max(0.001), 
					false
				);
			}
		);
	}
	
	
	drawContents { |scale = 1|
		
		var curves;
		var selectColor = Color.yellow;
		var pospt, times;
		var points, controls;
		
		Pen.use({	
			
			Pen.width = 0.164;
			Pen.color = Color.red(0.5, 0.5);
				
			//// draw configuration
			(WFSSpeakerConf.default ?? {
				WFSSpeakerConf.rect(48,48,5,5);
			}).draw;
				
				// draw center
			Pen.line( -0.25 @ 0, 0.25 @ 0 ).line( 0 @ -0.25, 0 @ 0.25).stroke;
				
				// draw contents
			Pen.scale( 1, -1 );	
					
			//// object
			
			points = object.positions.collect(_.asPoint);
			controls = if( object.respondsTo( \controls ) )
					{ object.controls }
					{ points.collect({ |item, i|
							points.clipAt( (i-1..i+2) ).collect(_.asArray).flop
								.collect({ |array|
								array.splineIntControls(1/3)
							}).flop.collect( _.asPoint);
						});	
					};
					
			curves = points.collect({ |item,i| [ item ] ++ controls[i] });
						
			// controls
			
			if( showControls ) {	
				Pen.color = Color.gray(0.35);
				Pen.width = scale * 0.25;
				
				points[1..].do({ |item, i|
					Pen.line( points[i], controls[i][0] );
					Pen.line( item, controls[i][1] );
					Pen.stroke;
					Pen.addArc( controls[i][0], 1.5 * scale, 0, 2pi );
					Pen.addArc( controls[i][1], 1.5 * scale, 0, 2pi );
					Pen.fill;
				});
			};
			
			
			// lines
			if( [0,1].includes( drawMode ) ) {
				Pen.width = scale * 0.5;
				Pen.color = Color.gray(0.25);
				Pen.moveTo( curves[0][0] );
				curves[1..].do({ |item, i|
					Pen.curveTo( item[0], *curves[i][1..] );
				});
				Pen.stroke;
			};
			
			// points
			if( drawMode != 3 ) {	
				Pen.width = scale;
				Pen.color = Color.green(0.5,0.5); // start point
				Pen.moveTo( curves[0][0] );
				Pen.addArc( curves[0][0], 4 * scale, 0, 2pi );
				Pen.fill;
				
				Pen.color = Color.red(1, 0.5); // end point
				Pen.moveTo( curves.last[0] );
				Pen.addArc( curves.last[0], 4 * scale, 0, 2pi );
				Pen.fill;
			};
			
			if( drawMode == 4 ) {	
				times = object.times / object.times.mean;
				Pen.color = Color.gray(0.25);
				curves[1..].do({ |item, i|
					var n, pt1;
					n = times[i] * 10;
					pt1 = [ curves[i][0], item[0] ].splineIntPart1( *curves[i][1..] );
					n.do({ |ii|
						Pen.addArc( pt1.splineIntPart2( ii / n ), scale, 0, 2pi );
					});
				});
				Pen.fill;
			};
			
			// selected
			Pen.use({	
				if( selected.notNil ) {	
					Pen.width = scale * 2;
					Pen.color = selectColor;
					selected.do({ |item|
						Pen.moveTo( curves[item][0] );
						Pen.addArc( curves[item][0] , 4 * scale, 0, 2pi );
					});
					
					Pen.fill;
				};
			});
			
			// show position
			if( pos.notNil ) {
				pospt = object.atTime2( pos, 'hermite', loop: false ).asPoint;
				Pen.color = Color.black.alpha_(0.5);
				Pen.moveTo( pospt );
				Pen.addArc( pospt , 5 * scale, 0, 2pi );
				Pen.fill;
			};
			
			if( [0,2,4].includes( drawMode ) ) {	
				Pen.color = Color.blue(1,0.5);
				curves[1..curves.size-1].do({ |item|
					Pen.moveTo( item[0] );
					Pen.addArc( item[0], 3 * scale, 0, 2pi );
				});
				Pen.stroke;
			};
			
		});
		
	}
	
	getNearestIndex { |point, radius| // returns nil if outside radius
		^object.positions.detectIndex({ |pt, i|
			pt.asPoint.dist( point ) <= radius
		});
	}
	
	getIndicesInRect { |rect|
		var pts = [];
		object.positions.do({ |pt, i|
			if( rect.contains( pt.asPoint ) ) { pts = pts.add(i) };
		});
		^pts;					
	}
	
	// general methods
	
	path_ { |path| this.object = path }
	path { ^object }
	
	pos_ { |newPos|
		pos = newPos;
		{ this.refresh; }.defer; // for animation
		this.changed( \pos );
	}
	
	undoKeys { ^[ \mouse_edit, \new_object, \removeSelected, \moveSelected, 
			\scaleSelected, \rotateSelected, \duplicateSelected, \endRecord
		]; 
	}
	
	// changing the object
	
	moveSelected { |x = 0,y = 0, update = true|
		if( selected.size > 0 ) {
			selected.do({ |index|
				object.positions[ index ] = object.positions[ index ] + (x@y);
			});
			this.refresh;
			if( update ) { this.changed( \moveSelected ); };
		};
	}
	
	scaleSelected { |x = 1, y, update = true|
		y = y ? x;
		if( selected.size > 0 ) {
			selected.do({ |index|
				object.positions[ index ] = object.positions[ index ] * (x@y);
			});
			this.refresh;
			if( update ) { this.changed( \scaleSelected ); };
		};
	}
	
	rotateSelected { |angle = 0, scale = 1, update = true|
		if( selected.size > 0 ) {
			selected.do({ |index|
				object.positions[ index ] = object.positions[ index ].rotate( angle ) * scale;
			});
			this.refresh;
			if( update ) { this.changed( \rotateSelected ); };
		};
	}
	
	duplicateSelected { 
		var points, times;
		if( selected.size >= 1 ) {
			selected = selected.sort;
			points = object.positions[ selected ];
			times = object.times[ selected ];
			times = times.collect({ |item, i|
				item ?? { object.times[ selected[i]-1 ] };
			});
			object.positions = 
				object.positions[..selected.last] ++
				points ++ 
				object.positions[selected.last+1..];
			object.forceTimes(
				object.times[..selected.last] ++
				times ++ 
				object.times[selected.last+1..];
			);
			selected = (0..points.size-1) + selected.last + 1;
			this.refresh;
			this.changed( \duplicateSelected );
		};
	}
	
	removeSelected {
		object.positions = object.positions.select({ |item, i|
			selected.includes(i).not;
		});
		object.forceTimes( 
			object.times.select({ |item, i|
				selected.includes(i).not;
			}) 
		);
		selected = [];
		this.refresh;
		this.changed( \removeSelected );
	}
	
	// selection
	
	select { |...indices|
		if( indices[0] === \all ) { 
			indices = object.positions.collect({ |item, i| i }).flat; 
		} { 
			indices = indices.flat;
		};
		if( selected != indices ) {
			selected = indices; 
			this.refresh;
			this.changed( \select );
		};
	}
	
	selectNoUpdate { |...index|
		if( index[0] === \all ) { 
			index = object.positions.collect({ |item, i| i }).flat 
		} {
			index = index.flat;
		};
		if( selected != index ) {
			selected = index;
			this.changed( \select ); 
		};
	}
	
	// animation
	
	animate { |bool = true, startAt|
		var res = 0.05;
		animationTask.stop;
		if( bool ) {
			this.pos = pos ? startAt ? 0;
			animationTask = Task({
				while { pos.inclusivelyBetween(0, object.length) } {
					res.wait;
					this.pos = pos + (res * animationRate);
				};
				this.pos = nil;
			}).start;
		} {
			this.pos = nil;
		};
		this.changed( \animate, bool );
	}
	
	
	
	// recording support
	
	startRecord { |point, clearPath = true, addTime = 0.1|
		recordLastTime = Process.elapsedTime;
		if( clearPath ) { 
			object.positions = [ point.asWFSPoint ];
			object.forceTimes([]);
		} {
			object.positions = object.positions ++ [ point.asWFSPoint ];
			object.forceTimes( object.times ++ [ addTime ] );
		};
	}
	
	recordPoint { |point| // adds point to end
		var newTime, delta;
		if( recordLastTime.notNil ) { // didn't start recording yet
			newTime = Process.elapsedTime;
			object.forceTimes( object.times ++ [ newTime - recordLastTime ] );
			object.positions = object.positions ++ [ point.asWFSPoint ];
			recordLastTime = newTime;
		} { 
			"%: didn't start recording yet\n".postf( thisMethod ); 
		};	
	}
			
	endRecord {
		recordLastTime = nil;
		this.changed( \endRecord );
	}
	


}