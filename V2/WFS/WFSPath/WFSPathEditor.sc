WFSPathEditor {
	
	var <view, <pathView, <editView;
	var <editWidth = 167;
	var <>action;
	
	
	*new { |parent, bounds, object, addUndoManager = true|
		^super.new.init( parent, bounds, object, addUndoManager )
	}
	
	init { |parent, bounds, object, addUndoManager|
		
		var ctrl, ctrl2, ctrl3;
		
		if( parent.isNil ) { 
			bounds = bounds ?? { (420 + (editWidth + 4)) @ 516 }; 
		} {
			bounds = parent.asView.bounds;
		};
		
		view = EZCompositeView( parent, bounds, gap: 2@2, margin: 2@2 );
		view.resize_(5);
		bounds = view.asView.bounds;
		view.addFlowLayout(0@0, 2@2);
		
		object = object.asWFSPath2;
		
		pathView = WFSPathView( view, 
			bounds.copy.width_( bounds.width - (editWidth + 4) ), object );
		
		view.view.decorator.shift(0,14);
		
		editView = WFSPathEditView( view, editWidth @ bounds.height, object );
		
		editView.resize_(3);
		
		ctrl = SimpleController( pathView.xyView )
			.put( \select, { editView.selected = pathView.selected })
			.put( \mouse_hit, { 
				editView.apply( true )
			})
			.put( \mouse_edit, { 
				editView.object = pathView.object;
			});
			
		ctrl2 =  SimpleController( pathView.timeView )
			.put( \mouse_hit, { 
				editView.apply( true )
			})
			.put( \mouse_edit, { 
				editView.object = pathView.timeView.object;
			});
			
		ctrl3 = SimpleController( editView )
			.put( \apply, { pathView.edited( \numerical_edit ); } );
		
		view.onClose_( { ctrl.remove; ctrl2.remove; } );
		
		editView.action = { |ev, what|
			pathView.refresh;
			action.value(this);
		};
		
		pathView.action = { 
			action.value( this );
		};
		
		pathView.timeView.action = {
			action.value( this );
		};
	
	}
	
	object { ^pathView.object }
	
	object_ { |obj| pathView.object = obj; editView.object = obj }
	
}