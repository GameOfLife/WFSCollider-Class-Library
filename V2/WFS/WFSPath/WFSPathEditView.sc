WFSPathEditView {
	
	var <object;
	var <objectCopy;
	var <>editFuncs;
	var <view, <views;
	var <>action;
	var <>duplicateAction;
	
	*new { |parent, bounds, object, editDefs|
		^super.new.init( parent, bounds, object, editDefs )
	}
	
	init { |parent, bounds, inObject, editDefs|
		
		object = inObject ? object;
		object = object.asWFSPath2;
		this.makeObjectCopy;
		
		
		if( parent.isNil ) {
			bounds = bounds ?? { 177 @ 280 };
		};
		
		view = EZCompositeView( parent, bounds, true, 2@2, 2@2 );
		editFuncs = this.makeEditFuncs( editDefs );
		
		this.makeViews;
		
		view.view.decorator.nextLine;
		
		view.decorator.shift( 55, 0 );
		
		views[ \apply ] = SmoothButton( view, 40@14 )
			.font_( Font( Font.defaultSansFace, 10 ) )
			.label_( "apply" )
			.border_( 1 )
			.radius_( 2 )
			.action_({ 
				this.apply( true );
				action.value( this, \apply )
			});
			
		views[ \reset ] = SmoothButton( view, 40@14 )
			.font_( Font( Font.defaultSansFace, 10 ) )
			.label_( "reset" )
			.border_( 1 )
			.radius_( 2 )
			.action_({ 
				this.reset;
				action.value( this, \reset );
			});
			
		view.decorator.nextLine;
		
		view.decorator.shift( 55, 0 );
			
		views[ \duplicate ] = SmoothButton( view, 82@14 )
			.font_( Font( Font.defaultSansFace, 10 ) )
			.label_( "duplicate" )
			.border_( 1 )
			.radius_( 2 )
			.action_({ 
				if( duplicateAction.notNil ) {
					duplicateAction.value( this );
				} {
					WFSPathEditView( object: object.deepCopy );
				};
			});
			
		
		view.view.bounds = view.view.bounds.height_( view.view.children.last.bounds.bottom );
		
		this.resetFuncs;
		
	}
	
	makeObjectCopy {
		if( object.isKindOf( WFSPathURL ) ) {
			objectCopy = object.wfsPath.deepCopy; // copy the associated trajectory
		} {
			objectCopy = object.deepCopy;
		}
	}
	
	selected {
		^editFuncs[0].selection;
	}
	
	selected_ { |selected|
		editFuncs.do( _.selection_(selected) );
	}
	
	revertObject { 
		object.positions = objectCopy.positions;
		object.times = objectCopy.times;
		object.type = objectCopy.type;
		object.curve = objectCopy.curve;
		object.clipMode = objectCopy.clipMode;
	}
	
	object_ { |newObject|
		object = newObject;
		this.resetFuncs;
		this.makeObjectCopy;
	}
	
	resize_ { |resize|
		view.resize_( resize )
	}
	
	apply { |final = true, active = false|
		
		if( this.checkBypass.not ) {	
			this.revertObject;
			
			editFuncs.do({ |func|
				func.value( object );
			});
			
			if( final ) {
				this.resetFuncs;
				this.makeObjectCopy;
				this.changed( \apply );
			};
		};
	
		^object;
	}
	
	resetFuncs {
		editFuncs.do({ |func|
			func.reset( object );
		});
	}
	
	checkBypass { 
		var bypass = true;
		editFuncs.do({ |func|
			if( func.checkBypass( object ).not ) {
				bypass = false;
			};
		});
		^bypass;
	}
	
	reset {
		this.revertObject;
		this.resetFuncs;
		this.changed( \reset );
	}
	
	makeViews {
		views = ();
		
		editFuncs.collect({ |func|
			var key;
			key = func.defName;
			func.action = { 
				this.apply( false );
				action.value( this, key );
			};
			views[ key ] = func.makeViews( view, view.bounds );
			view.view.decorator.nextLine;
		});
		 
	}
	
	makeEditFuncs { |editDefs|
		^(editDefs ?? { [ 
			\name, \type, \move, \scale, \rotate, \smooth, \size, \duration, \equal, \reverse 
		] })	
			.asCollection 
			.collect(WFSPathEdit(_))
	}
	
}

WFSPathGeneratorView : WFSPathEditView {
	
	makeEditFuncs { |editDefs|
		^[ WFSPathEdit( \simpleSize ),  WFSPathEdit( \duration ) ] ++
			(editDefs ?? { [ 
				\circle 
			] })	
				.asCollection 
				.collect(WFSPathGenerator(_))
	}
	
	resetFuncs {
		editFuncs.do({ |func, i|
			func.reset( object );
			if( i >= 2 ) {
				func.blend = 0;
			};
		});
	}
	
}


+ WFSPath2 {
	edit { |parent, bounds| ^WFSPathEditor( parent, bounds, this ) }
}

+ WFSPathURL {
	edit { |parent, bounds| ^WFSPathEditor( parent, bounds, this.wfsPath ) }
}
