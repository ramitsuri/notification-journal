# cron with
# @reboot path/to/run.sh
tmux kill-session -t journal-server
tmux new-session -d -s 'journal-server' 'python main.py'