#!/bin/tcsh

set f = "log"
if ($# > 0) then
  set f = $1
  echo "file $f"
endif
# $3 for LAS $4 for UAS
grep 'averaged LAS' $f | tail -n +10 | awk '{print $4}' | head -2500 >! tt
# grep 'logpost' log | tail -n +10 | awk '{print $4}' > tt.post
# wc -l tt tt.post

rm -f ttt
touch ttt
# echo 'set y2range [-2000:*]' >> ttt
#echo 'set y2label "loglike"' >> ttt
echo 'set ylabel "UAS"' >> ttt
#echo 'set y2tics border' >> ttt
echo 'f(x) = (a*x+b)/(c*x+d)' >> ttt
echo 'fit f(x) "tt" via a,b,c,d' >> ttt
echo 'plot "tt" with lines t "UAS", f(x) notitle' >> ttt
# echo 'plot "tt" with lines t "LAS", "tt" w l smooth bezier notitle' >> ttt
# echo 'plot "tt" with lines notitle, "tt.post" with lines notitle axes x1y2' >> ttt
echo 'pause -1' >> ttt
gnuplot ttt

