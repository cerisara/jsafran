#!/bin/tcsh

grep logpost log | grep -v -e 'iter -1' | awk '{print $4}' > ll.like
grep logpost log | grep -v -e 'iter -1' | awk '{print $6}' > ll.las
grep logpost log | grep -v -e 'iter -1' | awk '{print $8}' > ll.uas

set nl = ""`awk '{print NR}' ll.like | tail -1`
echo $nl
exit

# goto deb

grep LAS log | grep -v -e averaged | tail -n +2 | awk '{print $6}' > klkl
echo 'plot "klkl" with lines' > klkll
echo 'pause -1' >> klkll
gnuplot klkll


deb:

grep logpost log | tail -n +2 | awk '{print $4}' > klkl
echo 'plot "klkl" with lines' > klkll
echo 'pause -1' >> klkll
gnuplot klkll

