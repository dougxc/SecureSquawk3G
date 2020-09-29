#
# This is an awk script to filter out the 2 most interesting loading
# stats from the trace output of the loader.
#
/^--* SuiteLoader stats for: /  { printf "\nSuite \"%s\"\n", $5; next; }
/^Total memory needed/          { printf "  Dynamic memory required: %s\n", $5; next; }
/^Total memory/                 { printf "  EEPROM memory used:      %s\n", $4; next; }

