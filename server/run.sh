# cron with
# @reboot path/to/run.sh
tmux has-session -t 'journal-server' 2>/dev/null
if [ $? != 0 ]; then
  tmux new-session -d -s 'journal-server' 'python main.py'
else
  tmux kill-session -t journal-server
  tmux new-session -d -s 'journal-server' 'python main.py'
fi