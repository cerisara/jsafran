#!/usr/bin/perl

$nbuf = 10;
open (FILE, 'log');
while (<FILE>) {
	if ($_ =~ /logpost/ && $_ !~ /iter -1/) {
		@cols = split(/ /, $_);
		push(@buf,$cols[5]);
		$bufsize = @buf;
		while ($bufsize > $nbuf) {
			shift(@buf);
			$bufsize = @buf;
		}
		if ($bufsize==$nbuf) {
			$sum=0;
			$sum += $_ for @buf;
			$sum /= $nbuf;
			print "$sum\n";
		}
	}
}

