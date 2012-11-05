% Analysis of data log from kinesutra
% 11/3/2012 - Brent Townshend
% Reads data from 'poses.txt' or 'poses_*.txt'
% Plots kinect skeleton positions of references and targets
thresh=0.1;
dir='../Kinect/Processing/KineSutra_Processing/';
x=load([dir,'poses.txt']);
t=unique(x(:,1));
setfig('poses'); clf;
col='rgbcymk';
%       1       2      3           4        5       6           7        8       9       10     11      12      13     14      15 
labels={'Head','Neck','LShoulder','LElbow','LHand','RShoulder','RElbow','RHand','Torso','LHip','LKnee','LFoot','RHip','RKnee','RFoot'};
ref   = [2,     9,     9,          3,       4,      9,          6,       7,      0,      9,     10,     11,     9,     13,     14];
dims={'x','y','z'};
dirs={'positive','negative'};
pnum=1;
for i=1:10:60
  sel=find(x(:,2)==i-1);
  data=x(sel,:);
  fprintf('\nSample %d, Time=%f\n',data(1,2),data(1,1));
  joints=data(:,3)+1;
  xt=nan(length(labels),3);
  xref=xt;
  pmove=xt;
  xt(joints,:)=data(:,4:6);
  xref(joints,:)=data(:,7:9);
  pmove(joints,:)=data(:,10:12);
  
  subplot(1,3,1);
  title('Reference');
  for j=1:length(joints)
    if all(xref(j,:)==0)
      fprintf('No data for %s\n',labels{joints(j)});
    else
      text(xref(j,1),xref(j,2),labels{joints(j)});
    end
  end

  subplot(1,3,pnum);
  pnum=pnum+1;

  hold on;
  axis equal

  xtn=xt;
  for j=1:length(ref)
    if ref(j)==0
      xtn(j,:)=xref(j,:);
    else
      limblen=norm(xt(j,:)-xt(ref(j),:));
      reflimblen=norm(xref(j,:)-xref(ref(j),:));
      fprintf('Limbscale %d->%d = %.2f  (%.1f,%.1f,%.1f)=%.1f  (%.1f,%.1f,%.1f)=%.1f pmove=(%.1f,%.1f,%.1f)\n', j, ref(j), reflimblen/limblen,xt(j,:)-xt(ref(j),:),limblen, xref(j,:)-xref(ref(j),:),reflimblen,pmove(j,:));
      xtn(j,:)=(xt(j,:)-xt(ref(j),:))*reflimblen/limblen+xref(ref(j),:);
    end
  end
  %xta=xt;
  %    xta(:,4)=1;
  %    tfm=xta(central,:)\xref(central,:);
  %xtn=xta*tfm;
  for j=1:size(xt,1)
    %xtn(j,:)=xtn(j,:)-mean(xt-xref);
    plot([xt(j,1),xref(j,1)],[xt(j,2),xref(j,2)],'c');
    plot([xtn(j,1),xref(j,1)],[xtn(j,2),xref(j,2)],'g');
    plot(xref(j,1)+[0,pmove(j,1)],xref(j,2)+[0,pmove(j,2)],'r');
  end
  title(sprintf('RMS=(%.1f,%.1f)\n',sqrt(mean(mean((xt(:,1:2)-xref(:,1:2)).^2))),sqrt(mean(mean((xtn(:,1:2)-xref(:,1:2)).^2)))));
  move=xtn-xref;
  move(:,3)=0;  % No z-dir moves
  [maxval,maxind]=max(abs(move(:)));
  [joint,dir]=ind2sub(size(xtn),maxind);
  if maxval>thresh
    fprintf('Move(%d)=(%.1f,%.1f,%.1f), pmove=(%.1f,%.1f,%.1f)\n', joint,move(joint,:),pmove(joint,:));
    fprintf('Move %s in %s %s direction by %.1f\n', labels{joint},dims{dir}, dirs{(move(joint,dir)>0)+1},maxval);
  else
    fprintf('In correct position!\n');
  end

  plot(xref(:,1),xref(:,2),'o');
end
