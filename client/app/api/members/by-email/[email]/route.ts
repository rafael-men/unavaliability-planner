import { NextRequest } from 'next/server';
import { proxy } from '../../../../lib/backend';

export const GET = (req: NextRequest) => proxy(req);
