ArrayEditView : WFSBasicEditView {
	
	var <>canChangeAmount = true;
	var <spec;
	
	// object is an array of points

	defaultObject	{ ^(0,0.1..1)	}
	defaultSpec { ^ControlSpec(0,1); }
	
	setDefaults {
		object = object ?? { this.defaultObject };
		view
			.keepRatio_(false)
			.gridLines_([0,0])
			.maxZoom_(8)
			.scale_([1,1])
			.move_([0.5,0.5]);
	}
	
	mouseEditSelected { |newPoint, mod|
		var pt;
		// returns true if edited
		if( mod.isKindOf( ModKey ).not ) {
			mod = ModKey( mod ? 0);
		};
		switch( editMode,
			\move,  { 
				pt = (newPoint.round(round) - lastPoint.round(round)) * (1@(1));
				this.moveSelected( pt.x, pt.y, mod, \no_undo );
			},
			\scale, { 
				pt = [ lastPoint.round(round).abs.max(0.001) * 
						lastPoint.asArray.collect({ |item|
							(item > 0).binaryValue.linlin(0,1,-1,1)
						}).asPoint,
					  newPoint.round(round).abs.max(0.001) * 
						newPoint.asArray.collect({ |item|
							(item > 0).binaryValue.linlin(0,1,-1,1)
						}).asPoint
				]; // prevent inf/nan
				pt = pt[1] / pt[0];
				this.scaleSelected( pt.x, pt.y, mod, \no_undo ); 
			},
			\rotate, { 
				this.rotateSelected( 
					lastPoint.angle - newPoint.angle, 
					1, 
					mod,
					\no_undo
				);
			},
			\rotateS, { 
				this.rotateSelected( 
					lastPoint.theta - newPoint.theta, 
					newPoint.rho.max(0.001) / lastPoint.rho.max(0.001),
					mod, 
					false
				);
			},
			\elastic, {
				pt = (newPoint.round(round) - lastPoint.round(round)) * (1@(-1));
				this.moveElastic( pt.x, pt.y, mod, \no_undo );
			},
			\twirl, {
				pt = (newPoint.round(round) - lastPoint.round(round)) * (1@(-1));
				this.moveTwirl( pt.x, pt.y, mod, \no_undo );
			},
			\chain, {
				pt = (newPoint.round(round) - lastPoint.round(round)) * (1@(-1));
				this.moveChain( pt.x, pt.y, mod, \no_undo );
			}
		);
	}
	
	setDragHandlers {
		view.view
			.beginDragAction_({ object })
			.canReceiveDragHandler_({ |vw|
				var drg = View.currentDrag;
				if( drg.isString ) {
					drg = { drg.interpret }.try;
				};
				case { drg.isArray } {
					drg.collect(_.asPoint).every(_.isKindOf( Point ) );
				} { drg.isKindOf( Point ) } {
					true
				} { drg.respondsTo(\asWFSPointGroup) && {drg.isArray.not} } {
					true
				} {
					false;
				};
			})
			.receiveDragHandler_({ |vw|
				var drg = View.currentDrag;
				if( drg.isString ) {
					drg = drg.interpret;
				};
				case { drg.isKindOf( WFSPointGroup ) } {
					this.points = drg.positions;
				} { drg.isKindOf( Point ) } {
					this.points = [ drg ];
				} { drg.isArray } {
					this.points = drg;
				} { this.points = drg.asWFSPointGroup };
				this.edited( \drag_dropped_points );
			});
	 }
	
	drawContents { |scale = 1|
		var points, controls;
		var selectColor = Color.yellow;
		var spc;
		
		//scale = scale.asArray.mean;
		
		spc = spec ? this.defaultSpec;
		
		view.fromBounds = Rect( 0, 1.1, this.object.size, -1.2 );
		
		Pen.use({	
			
			points = this.points.asCollection.collect(_.asPoint);
			
			Pen.color = Color.blue(0.5,0.05);
			Pen.fillRect( Rect(0,0,this.object.size,1) );
			
			Pen.color = Color.blue(0.5,0.33);
			points.do({ |item|
					Pen.addRect( 
						Rect( item.x-0.5, 0.5, 1, item.y - 0.5 );
					);
			});
			Pen.fill;	
		
			Pen.color = Color.blue(0.5,0.75);
			points.do({ |item|
					Pen.addOval( Rect.aboutPoint( item, scale.x * 5, scale.y * 5 ) );
			});
			Pen.fill;	
		
			// selected
			Pen.use({	
				if( selected.notNil ) {	
					//Pen.width = scale;
					Pen.color = selectColor;
					selected.do({ |item|
						Pen.addOval( 
							Rect.aboutPoint( points[item], scale.x * 4, scale.y * 4 ) 
						);
					});
					
					Pen.fill;
				};
			});			
		});
		
	}
	
	getNearestIndex { |point, scaler| // returns nil if outside radius
		var radius, rect;
		point = point * (1 @ -1);
		rect = Rect.aboutPoint( point, scaler.x.abs * 5, scaler.y.abs * 5 );
		^this.points.detectIndex({ |pt, i|
			rect.contains( pt.asPoint );
		});
	}
	
	getIndicesInRect { |rect|
		var pts = [];
		rect = rect.scale(1@(-1));
		this.points.do({ |pt, i|
			if( rect.contains( pt.asPoint ) ) { pts = pts.add(i) };
		});
		^pts;					
	}
	
	handleUndo { |obj|
		if( obj.notNil ) {
			object = obj;
			externalEdit = true;
			this.refresh;
			this.edited( \undo, \no_undo );
		};
	}
	
	// general methods
	
	resize { ^view.resize }
	resize_ { |resize| view.resize = resize }
	
	point_ { |point| this.object = (object ? [0]).asCollection[0] = point.asPoint }
	point { ^this.points[0] }
	
	points_ { |points|
		if( points.isKindOf( WFSPointGroup ) ) {
			points = points.positions.deepCopy;
		} {
			points = points.asCollection.collect(_.asPoint);
		};
		if( canChangeAmount ) {
			this.object = points;
		} {
			this.object = this.object.collect({ |item, i|
				points[i] ?? { object[i] };
			});
		};
	}
	
	points { ^object.collect({ |item,i|
			(i+0.5) @ item.clip(0,1);
		});
	}
	
	at { |index| ^this.points[index] }
	
	zoomToFit { |includeCenter = true|
		view.scale = [1,1];
		view.move = [0.5,0.5];
		//view.viewRect = 
	}
	
	zoomToRect { |rect|
		rect = rect ?? { 
			object.asRect.union( Rect(0,0,0,0) ).insetBy(-1,-1) 
		};
		view.viewRect = rect;
	}
		
	// changing the object
	
	moveSelected { |x = 0,y = 0, mod ...moreArgs|
		if( selected.size > 0 ) {
			if( mod.ctrl && { selected.size == 1 } ) {
				selected.do({ |index|
					this.object[index] = this.object[index] + y;
				});
			} {
				selected.do({ |index|
					this.object[index] = this.object[index] + y;
				});
			};
			this.refresh; 
			this.edited( \edit, \move, *moreArgs  );
		};
	}
	
	moveElastic { |x = 0,y = 0, mod ...moreArgs|
		var selection;
		if( selected.size > 0 ) {
			
			selection = (selected.minItem..selected.maxItem);
			
			selection.do({ |index|
				var pt;
				pt = this.points[ index ];
				if( pt.notNil ) {
					pt.x = pt.x + x;
					pt.y = pt.y + y;
				};
			});
			
			2.do({ |ii|
				var rest, restSize;
				if( ii == 0 ) {
					rest = (..selection[0]);
				} {
					rest = (selection.last..this.points.size-1).reverse;
				};
				rest = rest[..rest.size-2];
				restSize = rest.size;
				rest.do({ |index, i|
					var pt, factor;
					pt = this.points[ index ];
					if( pt.notNil ) {
						factor = (i/restSize);
						pt.x = pt.x + (x * factor);
						pt.y = pt.y + (y * factor);
					};
				});	
			});
			
			this.refresh; 
			this.edited( \edit, \move, *moreArgs );
		};
	}
	
	moveTwirl { |x = 0,y = 0, mod ...moreArgs|
		var selection, angles, rhos, firstPoint, lastPoint;
		if( selected.size > 0 ) {
			
			selection = (selected.minItem..selected.maxItem);
			
			firstPoint = this.points[selection[0]];
			lastPoint = this.points[selection.last]; 
			
			angles = [ 
				(firstPoint + (x@y)).angle - firstPoint.angle,
				(lastPoint + (x@y)).angle - lastPoint.angle
			].wrap(-pi, pi);
			
			rhos = [ 
				(firstPoint + (x@y)).rho - firstPoint.rho,
				(lastPoint + (x@y)).rho - lastPoint.rho
			];
			
			selection.do({ |index|
				var pt;
				pt = this.points[ index ];
				if( pt.notNil ) {
					pt.x = pt.x + x;
					pt.y = pt.y + y;
				};
			});
			
			2.do({ |ii|
				var rest, restSize;
				if( ii == 0 ) {
					rest = (..selection[0]);
				} {
					rest = (selection.last..this.points.size-1).reverse;
				};
				rest = rest[..rest.size-2];
				restSize = rest.size;
				rest.do({ |index, i|
					var pt, factor, newPoint;
					pt = this.points[ index ];
					if( pt.notNil ) {
						factor = (i/restSize);
						newPoint = pt.asPolar;
						newPoint.theta = newPoint.theta + (angles[ii] * factor);
						newPoint.rho = (newPoint.rho + (rhos[ii] * factor)).abs;
						newPoint = newPoint.asPoint;
						pt.x = newPoint.x;
						pt.y = newPoint.y;
					};
				});	
			});
			
			this.refresh; 
			this.edited( \edit, \move, *moreArgs );
		};
	}
	
	moveChain { |x = 0,y = 0, mod ...moreArgs|
		var selection, data;
		// keeps fixed distances between points
		if( selected.size > 0 ) {
			
			selection = (selected.minItem..selected.maxItem);
			
			data = 2.collect({ |ii|
				var rest, restSize, distance;
				if( ii == 0 ) {
					rest = (..selection[0]).reverse;
				} {
					rest = (selection.last..this.points.size-1);
				};
				
				distance = rest[1..].collect({ |item, i|
					this.points[item].dist( this.points[rest[i]] );
				});
				
				[ rest, distance ];
			});
			
			selection.do({ |index|
				var pt;
				pt = this.points[ index ];
				if( pt.notNil ) {
					pt.x = pt.x + x;
					pt.y = pt.y + y;
				};
			});
			
			data.do({ |data|
				var rest, distances;
				#rest, distances = data;
				rest[1..].do({ |index, i|
					var pt, polar;
					pt = this.points[ index ];
					if( pt.notNil ) {
						polar = (pt - this.points[rest[i]]).asPolar;
						polar.rho = distances[i];
						polar = polar.asPoint + this.points[rest[i]];
						pt.x = polar.x;
						pt.y = polar.y;
					};
				});	
			});
			
			this.refresh; 
			this.edited( \edit, \move, *moreArgs );
		};
	}
	
	scaleSelected { |x = 1, y, mod ...moreArgs|
		y = y ? x;
		if( selected.size > 0 ) {
			selected.do({ |index|
				var pt;
				pt = this.points.asCollection[ index ];
				pt.x = pt.x * x;
				pt.y = pt.y * y;
			});
			this.refresh;
			this.edited( \edit, \scale, *moreArgs );
		};
	}
	
	rotateSelected { |angle = 0, scale = 1, mod ...moreArgs|
		if( selected.size > 0 ) {
			selected.do({ |index|
				var pt, rpt;
				pt = this.points.asCollection[ index ];
				rpt = pt.rotate( angle ) * scale;
				pt.x = rpt.x;
				pt.y = rpt.y;
			});
			this.refresh;
			this.edited( \edit, \rotate, *moreArgs );
		};
	}
	
	duplicateSelected { 
		var points;
		if( canChangeAmount && { selected.size >= 1} ) {
			selected = selected.sort;
			points = this.points.asCollection[ selected ].collect(_.copy);
			selected = object.size + (..points.size-1);
			this.points = this.points ++ points;
			this.refresh;
			this.edited( \duplicateSelected );
		};
	}
	
	removeSelected {
		if( canChangeAmount && { object.size > selected.size } ) {
			this.points = this.points.select({ |item, i|
				selected.includes(i).not;
			});
			selected = [];
		} {
			"WFSPointView-removeSelected : should leave at least one point".warn;
		};
		this.refresh;
		this.edited( \removeSelected );
	}
	
	// selection
	
	select { |...indices|
		if( indices[0] === \all ) { 
			indices = this.points.asCollection.collect({ |item, i| i }).flat; 
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
			index = this.points.asCollection.collect({ |item, i| i }).flat 
		} {
			index = index.flat;
		};
		if( selected != index ) {
			selected = index;
			this.changed( \select ); 
		};
	}
}

