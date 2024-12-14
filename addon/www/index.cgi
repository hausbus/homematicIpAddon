#!/bin/tclsh

set version [loadFile VERSION]

set content [loadFile index.template.html]
regsub -all {<%version%>} $content $version content

puts $content
