+ EZGui {
	*makeParentView { |parent, bounds|
		var new, view, margin, gap;
		// extracts the window making capacities of EZGui, disregards the rest
		new = this.new;
		#view, bounds = new.prMakeMarginGap(parent).prMakeView(parent, bounds ? (350@20) );
		margin = new.slotAt( \margin );
		gap = new.slotAt( \gap );
		^[ view, bounds, margin, gap ]
	}
}