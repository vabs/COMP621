a = zeros(10001, 1);
d = zeros(10001, 1);
x = zeros(10001, 1);
tic();
for i = (1 : 1 : 5000)
  d((i + 1)) = (d(i) + 20);
x((1 : 1 : 5000)) = a(((1 : 1 : 5000) + 1));
end
x((1 : 1 : 5000)) = a(((1 : 1 : 5000) + 1));
toc();
