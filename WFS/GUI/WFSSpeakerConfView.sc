WFSSpeakerConfView : WFSBasicEditView {
	
	defaultObject	{ ^nil } // nil means default
	
	conf { ^object ? WFSSpeakerConf.default; }
	
	drawContents { |scale = 1|
		var conf, lines;
		var count = 0;
		var letters;
		
		scale = scale.asArray.mean;
	
		
		conf = this.conf;
		
		if( conf.notNil ) {
			Pen.use({
				// show selection
				Pen.scale(1,-1);
				lines = conf.asLines;
				
				if( selected.size > 0 ) {
					
					Pen.width = 0.164 * 2;
					Pen.color = Color.yellow;
					
					lines.do({ |line, i|
						if( selected.includes( i ) ) {
							Pen.line( *line );
						};
					});
					Pen.stroke;
					
				};
				
				Pen.color = Color.black;
				
				letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
				
				conf.arrayConfs.do({ |arrayConf, i|
					var points, counts, movePt;
					points = [ arrayConf.lastPoint, arrayConf.firstPoint ];
					counts = count + [0, arrayConf.n-1 ];
					count = count + arrayConf.n;
					movePt = Polar(12, arrayConf.angle.neg ).asPoint;
					
					Pen.color = Color.black.alpha_(0.33);
					Pen.font = Font( Font.defaultSansFace, 9 );
					points.do({ |item, ii|
						Pen.use({
							Pen.translate( item.x, item.y );
							Pen.scale(scale,scale.neg);
							Pen.stringCenteredIn( 
								counts[ii].asString,
								Rect.aboutPoint( movePt, 20, 15)
							);
						});
					});
					
					Pen.color = Color.red(0.5, 0.5);
					Pen.font = Font( Font.defaultSansFace, 14 );
					Pen.use({
						Pen.translate( *(arrayConf.centerPoint).asArray );
						Pen.scale(scale,scale.neg);
						Pen.stringCenteredIn( 
								letters[i].asString,
								Rect.aboutPoint( movePt, 22, 22)
							);
						
					});
					
				});
				
			});
			
			Pen.use({
				
				Pen.width = 0.164;
				Pen.color = Color.red(0.5, 0.5);
					
				//// draw configuration
				conf.draw;
				
			});
		};
		
	}
	
	select { |...indices|
		if( indices[0] === \all ) { 
			indices = object.positions.collect({ |item, i| i }).flat; 
		} { 
			indices = indices.flat.select(_.notNil);
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
			index = index.flat.select(_.notNil);
		};
		if( selected != index ) {
			selected = index;
			this.changed( \select ); 
		};
	}

	
	zoomToFit {
		view.viewRect_( 
			(this.conf).asRect.scale(1@(-1)).insetBy(-1,-1) 
		);  
	}
	
	getNearestIndex { |point, scaler| // returns nil if outside radius
		var radius;
		var conf;
		conf = this.conf;
		if( conf.notNil ) {
			radius = scaler.asArray.mean * 5;
			^conf.arrayConfs.detectIndex({ |arr, i|
				var pt;
				pt = point.rotate( arr.angle.neg );
				pt.x.inclusivelyBetween( arr.dist - radius, arr.dist + radius ) &&
				{
					pt.y.inclusivelyBetween( 
						arr.rotatedFirstPoint.y - radius,
						arr.rotatedLastPoint.y + radius
					);
				};
			});
		} {
			^nil;
		};
	}
	
	
	getIndicesInRect { |rect|
		var conf, index = [], corners;
		conf = this.conf;
		if( conf.notNil ) {
			conf.arrayConfs.do({ |arrayConf, i|
				var rct;
				// close enough for jazz..
				rct = Rect.fromPoints( arrayConf.firstPoint, arrayConf.lastPoint );
				if( rct.intersects( rect ) ) { index = index.add(i) };
			});
		};
		^index;			
	}
	
	mouseEditSelected {
	}
	
	
}